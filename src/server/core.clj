(ns server.core
  (:require
   org.httpkit.server))

(def players
  (atom {}))

(def buildings
  (atom {[15 4] [:b :r]
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

(defn broadcast-players-state
  []
  (doseq [[channel data] @players]
    (org.httpkit.server/send! channel (str {:event "players" :data (vals @players)}))))

(defn broadcast-buildings-state
  []
  (doseq [[channel data] @players]
    (org.httpkit.server/send! channel (str {:event "buildings" :data @buildings}))))

(defn handler
  [request]
  (cond
    (= "/ws" (:uri request))
    (org.httpkit.server/with-channel request channel
      (do 
        (swap! players assoc channel {:position {:x 0 :y 0} :name (str "Guest #" (inc (count @players))) :color (rand-nth ["red" "yellow" "green" "purple"])})

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
  (server)
  @players 
  )
