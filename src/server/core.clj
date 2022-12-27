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

         [14 5] [:b :u]
         [14 6] [:b :u]
         [14 7] [:b :u]
         [14 8] [:b :u]
         [14 9] [:b :u]
         [14 10] [:b :u]}))

(def tick-status
  (atom true))

(future-cancel tick)
(def tick
  (future
    (while @tick-status
      (doseq [[channel data] @players]
        (org.httpkit.server/send! channel (str {:event "players" :data (vals @players)}))
        (org.httpkit.server/send! channel (str {:event "buildings" :data @buildings})))
      (Thread/sleep 1))))

(defn handler
  [request]
  (cond
    (= "/ws" (:uri request))
    (org.httpkit.server/with-channel request channel
      (swap! players assoc channel {:position {:x 0 :y 0} :name (str "Guest #" (inc (count @players))) :color (rand-nth ["red" "yellow" "green" "purple"])})
      (org.httpkit.server/on-close
       channel
       (fn [status]
         (swap! players dissoc channel)))
      (org.httpkit.server/on-receive
       channel
       (fn [string]
         (let [data (read-string string)]
           (cond
             (= "change-name" (:event data))
             (swap! players assoc-in [channel :name] (:data data))
             (= "move-y" (:event data))
             (swap! players assoc-in [channel :position :y] (:data data))
             (= "move-x" (:event data))
             (swap! players assoc-in [channel :position :x] (:data data)))))))
    :else {:status 200}))

(comment
  (def server (org.httpkit.server/run-server #'handler {:port 8080}))
  (server)
  @players 
  (reset! tick-status false))
