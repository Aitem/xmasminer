(ns server.core
  (:require
   [org.httpkit.server]
   [server.buildings :as buildings])
  (:import [java.util.concurrent Executors TimeUnit])
  (:gen-class))


(def uid (atom 0))

(defn next-uid []
  (str (swap! uid inc)))

(def player-skins
  #{"p_1" "p01" "p_02" "p_03" "p_04"})


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

(defn fabric-rotate
  [cursor dir inputs]
  (prn cursor dir inputs)
  (when dir
    (map-indexed
     (fn [index input]
       [(case dir
          :r [(+ (:x cursor) index) (:y cursor)]
          :l [(- (:x cursor) index) (:y cursor)]
          :d [(:x cursor) (+ (:y cursor) index)]
          :u [(:x cursor) (- (:y cursor) index)])
        (first input)])
     inputs)))

(defn make-fabric
  [{[x y] :position direction :direction output :output inputs :inputs :as options}]
  (into {}
        (map-indexed
         (fn [index [tp cnt]]
           [(case direction
              :r [(+ x index) y]
              :l [(- x index) y]
              :d [x (+ y index)]
              :u [x (- y index)])
            (cond-> [:f :r (merge
                            {:input tp :amount cnt :main [x y] }
                            (when (= 0 index)
                              {:dir direction  :inputs inputs :ticks (:ticks options) :output (:output options)}))])])
         inputs)))

(defonce buildings 
  (atom (merge
         (make-fabric {:position [5 3] :direction :r :inputs {:b 2 :w 2 :c 2 :l 2} :output :b :ticks 2}) 
         {[3 3]   [:m :r :b :h]
          [3 4]   [:m :r :w :h]
          [3 5]   [:m :r :c :h]
          [3 6]   [:m :r :l :h]
          [25 4]  [:h :r :c :h {:limit 10 :count 0}]
          [15 15] [:h :r :c :h {:limit 10 :count 0}]

          ;; [15 16] [:f :a {:recept {:battery 2} :state {:battery 0} :ticks 2 :current-tick 0}] ;; accamulator = battery + battery
          ;; UI inputs ???
          ;; [15 17] [:f :c {:recept {:micro 1 :wire 1} :state {:micro 0 :wire 0} :ticks 2 :current 0}] ;; circuite = microproccessor + wire

          [10 10] [:q :u {:limit 10 :count 0}]

          })))

(defn add-building [x y building]
  (let [[building-type {direction :direction}] building]
    (swap! buildings
           assoc [x y] [(case building-type
                          :belt :b)
                        (case direction
                          :left :l
                          :right :r
                          :up :u
                          :down :d)])))

(add-building 15 4 (buildings/belt :right))
(add-building 16 4 (buildings/belt :right))
(add-building 17 4 (buildings/belt :right))
(add-building 18 4 (buildings/belt :right))
(add-building 19 4 (buildings/belt :right))
(add-building 20 4 (buildings/belt :right))
(add-building 14 4 (buildings/belt :right))

(add-building 21 4 (buildings/belt :down))
(add-building 21 5 (buildings/belt :down))
(add-building 21 6 (buildings/belt :down))
(add-building 21 7 (buildings/belt :down))
(add-building 21 8 (buildings/belt :down))
(add-building 21 9 (buildings/belt :down))
(add-building 21 10 (buildings/belt :down))

(add-building 15 11 (buildings/belt :left))
(add-building 16 11 (buildings/belt :left))
(add-building 17 11 (buildings/belt :left))
(add-building 18 11 (buildings/belt :left))
(add-building 19 11 (buildings/belt :left))
(add-building 20 11 (buildings/belt :left))
(add-building 21 11 (buildings/belt :left))

(add-building 14 5 (buildings/belt :up))
(add-building 14 6 (buildings/belt :up))
(add-building 14 7 (buildings/belt :up))
(add-building 14 8 (buildings/belt :up))
(add-building 14 9 (buildings/belt :up))
(add-building 14 10 (buildings/belt :up))
(add-building 14 11 (buildings/belt :up))

(defn broadcast-buildings-state
  []
  (let [flat-buildings
        (reduce (fn [acc [position building]]
                  (let [[x y] position
                        id (next-uid)
                        [type opts fab _ state] building]
                    (conj acc
                          {:id id
                           :x x
                           :y y
                           :type type
                           :data {:opts opts
                                  :fab fab
                                  :state state}}))) [] @buildings)]
    (doseq [[channel data] @players]
      (org.httpkit.server/send! channel (str {:event "buildings" :data flat-buildings})))))


(def mines
  {

   [-10 -10] [:b nil]
   [-10 -9] [:b nil]
   [-9 -10] [:b nil]
   [-9 -9] [:b nil]


   [-10 0] [:w nil]
   [-10 1] [:w nil]
   [-9 0]  [:w nil]
   [-9 1]  [:w nil]

   [0 -10] [:c nil]
   [0 -9]  [:c nil]
   [1 -10] [:c nil]
   [1 -9]  [:c nil]


   [10 -10] [:l nil]
   [10 -9]  [:l nil]
   [11 -10] [:l nil]
   [11 -9]  [:l nil]


   [10 0] [:m nil]
   [10 1] [:m nil]
   [11 0] [:m nil]
   [11 1] [:m nil]


   }
  )

;; RESOURCES
;; :b battery  +
;; :w wire     +
;; :c circuite +
;; :l light
;; :m microchip

(def resources
  (atom {
         [15 4]  [:b nil]
         [21 5]  [:w nil]
         [14 10] [:c nil]
         [20 11] [:l nil]
         }))

(defn broadcast-resources-state
  []
  (let [flat-resources
        (reduce (fn [acc [position resource]]
                  (let [[x y] position
                        id (next-uid)
                        [resource-type _] resource]
                    (conj acc {:x x
                               :y y
                               :id id
                               :type resource-type})))
                [] @resources)]
    (doseq [[channel data] @players]
      (org.httpkit.server/send! channel (str {:event "resources" :data flat-resources})))))

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
   (fn [acc [[x y] [_ dir type r]]]
     (merge acc
            (case dir
              :r  {[(inc x)  y] [type r]}
              :l  {[(dec x)  y] [type r]}
              :u  {[x  (dec y)] [type r]}
              :d  {[x  (inc y)] [type r]}
              {})
            ))
   {} miners)
  )

(defn spawn-on-fabric [miners]
  (reduce
   (fn [acc [[x y] [_ _dir type r]]]
     (assoc acc [(inc x)  y] [type r]))
   {} miners)
  )

(defn spawned-fabric-resource-position
  [dir [x y]]
  (case dir
    :r [x (dec y)]
    :l [x (dec y)]
    :d [(inc x) y]
    :u [(dec x) y]
    (prn "-------" dir)))


(defn process-res [[pos [t o]] gmap]
  (if-let [infra (get gmap pos)]
    (cond
      (= :h (first infra))
      (do
        (swap! buildings update pos (fn [hub] (update-in hub [4 :count] (fnil inc 0))))
        nil)
      (= :f (first infra))
      (let [opts (get-in infra [2])
            main-build (get @buildings (:main opts))]
        (do
          (when (= (:input opts) t)

            (swap! buildings update (:main opts)
                   (fn [main]
                     (let [current-count (or (get-in main [2 :storage t]) 0)]
                       (if (= current-count (get-in main [2 :inputs t]))
                         (assoc-in main [2 :done t] true)
                         (->
                          main
                          (assoc-in [2 :done t] false)
                          (assoc-in [2 :storage t] (inc current-count)))))))

            (when (= (count (get-in main-build [2 :inputs]))
                     (count (filter true? (vals (get-in @buildings [(:main opts) 2 :done])))))
              (if (>= (or (get-in main-build [2 :cticks]) 0)
                      (get-in main-build [2 :ticks]))
                (do 
                  (swap! buildings update-in [(:main opts) 2]
                         (fn [f] (assoc f :done nil :cticks 0 :storage nil)))
                  {(spawned-fabric-resource-position (get-in main-build [2 :dir])
                                                     (:main opts))
                   [(get-in main-build [2 :output]) nil]})
                (do (swap! buildings update-in [(:main opts) 2 :cticks] (fnil inc 0))
                    nil))))))
      :else
      (condp = infra
        [:b :r] {[(inc (first pos)) (second pos)] [t o]}
        [:b :l] {[(dec (first pos)) (second pos)] [t o]}
        [:b :u] {[(first pos) (dec (second pos))] [t o]}
        [:b :d] {[(first pos) (inc (second pos))] [t o]}
        {pos [t o]}))
    {pos [t o]}))

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
  (let [flat-mines
        (reduce
         (fn [acc [position mine]]
           (let [[x y] position
                 id (next-uid)
                 [resource-type & _] mine]
             (conj acc {:id id
                        :x x
                        :y y
                        :resource resource-type})))
         [] mines)]

    (doseq [[channel data] @players]
      (org.httpkit.server/send! channel (str {:event "mines" :data flat-mines})))))

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
               (cond
                 (= :fc (get-in data [:data :id]))
                 (swap! buildings merge (make-fabric
                                         {:position [(get-in data [:data :x]) (get-in data [:data :y])]
                                          :direction (get-in data [:data :dir])
                                          :inputs (get-in data [:data :inputs])
                                          :output (get-in data [:data :output])
                                          :ticks (get-in data [:data :ticks])}))
                 :else 
                 (swap! buildings assoc [(get-in data [:data :x]) (get-in data [:data :y])]
                        [(get-in data [:data :id])
                         (get-in data [:data :dir])]))
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


(defn -main [& args]
  (prn "RUN SERVER")
  (def server (org.httpkit.server/run-server #'handler {:port 8787}))
  (run-job ctx :global global-tick 1)
  )

(comment
  (def server (org.httpkit.server/run-server #'handler {:port 8080}))
  (run-job ctx :global global-tick 1)
  (server)
  @players 
  )
