(ns server.core
  (:require
   org.httpkit.server))

(def players
  (atom {}))

(def tick-status
  (atom true))

#_(future-cancel tick)
(def tick
  (future
    (while @tick-status
      (doseq [[channel data] @players]
        (org.httpkit.server/send!
         channel
         (str {:event "get-players" :data (vals @players)})))
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
           (prn data)
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
