(ns server.core
  (:require
   org.httpkit.server)
  (:import [java.util.concurrent Executors TimeUnit]))


(def uid (atom 0))

(defn next-uid []
  (str (swap! uid inc)))

(def player-skins
  #{"/img/p_1.png"
    "/img/p01.PNG"
    "/img/p_02.PNG"
    "/img/p_03.PNG"
    "/img/p_04.PNG"
    })


(defn stop-job [state job-id]
  (when-let [e (:executor (get @state job-id))]
    (.shutdown e))
  (when-let [f (:future (get @state job-id))]
    (future-cancel f)))

(defn run-job [state job-id job-fn & [interval]]
  (stop-job state job-id)
  (let [executor (Executors/newScheduledThreadPool 1)
        fut (.scheduleAtFixedRate executor job-fn 0 (or interval 5) TimeUnit/SECONDS)]
    (swap! state assoc job-id {:executor executor :future fut})))

(defonce ctx (atom {}))



(def players 
  (atom {}))


(def buildings
  (atom {[4 4]   [:m :r :c :h]
         [25 4]  [:h :r :c :h {:limit 10 :count 0}]
         [15 15] [:h :r :c :h {:limit 10 :count 0}]

         [15 16] [:f :a {:recept {:battery 2} :state {:battery 0} :ticks 2 :current-tick 0}] ;; accamulator = battery + battery
         ;; UI inputs ???
         [15 17] [:f :c {:recept {:micro 1 :wire 1} :state {:micro 0 :wire 0} :ticks 2 :current 0}] ;; circuite = microproccessor + wire

         [10 10] [:q :u {:limit 10 :count 0}]

         [15 4] [:b :r]
         [16 4] [:b :r]
         [17 4] [:b :r]
         [18 4] [:b :r]
         [19 4] [:b :r]
         [20 4] [:b :r]
         [14 4] [:b :r]

         [21 4] [:b :d]
         [21 5] [:b :d]
         [21 6] [:b :d]
         [21 7] [:b :d]
         [21 8] [:b :d]
         [21 9] [:b :d]
         [21 10] [:b :d]

         [15 11] [:b :l]
         [16 11] [:b :l]
         [17 11] [:b :l]
         [18 11] [:b :l]
         [19 11] [:b :l]
         [20 11] [:b :l]
         [21 11] [:b :l]

         [14 5] [:b :u]
         [14 6] [:b :u]
         [14 7] [:b :u]
         [14 8] [:b :u]
         [14 9] [:b :u]
         [14 10] [:b :u]
         [14 11] [:b :u]
         }))

(defn broadcast-buildings-state
  []
  (doseq [[channel data] @players]
    (org.httpkit.server/send! channel (str {:event "buildings" :data @buildings}))))


(def mines
  {
   [3 3] [:c :h]
   [3 4] [:c :h]
   [4 3] [:c :h]
   [4 4] [:c :h]
   }
  )

;; RESOURCES
;; :b battery
;; :w wire
;; :c chip
;; :l light

(def resources
  (atom {
         [15 4]  [:b nil]
         [21 5]  [:w nil]
         [14 10] [:c nil]
         [20 11] [:l nil]
         }))

(defn broadcast-resources-state
  []
  (doseq [[channel data] @players]
    (org.httpkit.server/send! channel (str {:event "resources" :data @resources}))))

(defn get-miners [buildings]
  (reduce
   (fn [acc [pos opts]]
     (if (= :m (first opts))
       (assoc acc pos opts)
       acc))
   {}
   buildings))

(defn spawn-on-miner [miners]
  (reduce
   (fn [acc [[x y] [_ _dir type r]]]
     (assoc acc [(inc x)  y] [type r]))
   {} miners)
  )


(defn process-res [[pos [t o]] gmap]
  (if-let [infra (get gmap pos)]
    (if (= :h (first infra))
      (do
        (swap! buildings update pos (fn [hub] (update-in hub [4 :count] inc)))
        nil)

      (condp = infra
        [:b :r] {[(inc (first pos)) (second pos)] [t o]}
        [:b :l] {[(dec (first pos)) (second pos)] [t o]}
        [:b :u] {[(first pos) (dec (second pos))] [t o]}
        [:b :d] {[(first pos) (inc (second pos))] [t o]}
        {pos [t o]}))

    {pos [t o]}
    )
  )

(defn global-tick []
  (try
    (let [gmap @buildings
          miners (get-miners gmap)
          spawned (spawn-on-miner miners)]
      ;; move resource
      (swap! resources
             (fn [ress]
               (reduce (fn [acc r] (merge acc (process-res r gmap))) {} ress)))
      ;; spawn resource
      (swap! resources merge spawned))

    (broadcast-resources-state)
    (broadcast-buildings-state)
    (catch Exception e
      (prn e)))
  )

(defn broadcast-mines-state
  []
  (doseq [[channel data] @players]
    (org.httpkit.server/send! channel (str {:event "mines" :data mines}))))

(defn broadcast-players-state
  []
  (doseq [[channel data] @players]
    (org.httpkit.server/send! channel (str {:event "players" :data (vals @players)}))))

(defn send-player-data [channel player-data]
  (org.httpkit.server/send! channel (str {:event "init" :data player-data})))

(defn handler
  [request]
  (cond
    (= "/ws" (:uri request))
    (org.httpkit.server/with-channel request channel
      (let [player-data {:position {:x 0 :y 0}
                         :name (str "Guest #" (inc (count @players)))
                         :skin (rand-nth (vec player-skins))
                         :color (rand-nth ["red" "yellow" "green" "purple"])
                         :id (next-uid)}]
        (swap! players assoc channel player-data)
        (send-player-data channel player-data)
        (broadcast-resources-state)
        (broadcast-mines-state)
        (broadcast-buildings-state)
        (broadcast-players-state))
      (org.httpkit.server/on-close
       channel
       (fn [status]
         (swap! players dissoc channel)))
      (org.httpkit.server/on-receive
       channel
       (fn [string]
         (let [data (read-string string)]
           (cond
             (= "remove-building" (:event data))
             (do 
               (swap! buildings dissoc [(get-in data [:data :x])
                                        (get-in data [:data :y])])
               (broadcast-buildings-state))
             (= "create-building" (:event data))
             (do 
               (swap! buildings assoc [(get-in data [:data :x]) (get-in data [:data :y])]
                      [(get-in data [:data :id])
                       (get-in data [:data :dir])])
               (broadcast-buildings-state))
             (= "change-name" (:event data))
             (do 
               (swap! players assoc-in [channel :name] (:data data))
               (broadcast-players-state))
             (= "move-y" (:event data))
             (do 
               (swap! players assoc-in [channel :position :y] (:data data))
               (broadcast-players-state))
             (= "move-x" (:event data))
             (do 
               (swap! players assoc-in [channel :position :x] (:data data))
               (broadcast-players-state)))))))
    :else {:status 200}))

(comment
  (def server (org.httpkit.server/run-server #'handler {:port 8080}))
  (run-job ctx :global global-tick 1)
  (server)
  @players 
  )
