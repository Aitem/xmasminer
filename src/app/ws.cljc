(ns app.ws
  (:require clojure.edn
            re-frame.core))

(re-frame.core/reg-event-db
 ::save-players
 (fn [db [_ players]]
   (assoc db :players players)))

(re-frame.core/reg-event-db
 ::save-mines
 (fn [db [_ data]]
   (assoc db :mines data)))

(re-frame.core/reg-event-db
 ::save-buildings
 (fn [db [_ buildings]]

   (doseq [a (js/document.getAnimations)]
     (set! (.-startTime a) 0))


   (assoc db :buildings buildings)))

(re-frame.core/reg-event-db
 ::save-resources
 (fn [db [_ payload]]
   (let [data
         (reduce (fn [acc resource]
                   (let [x (:x resource)
                         y (:y resource)
                         _id (:id resource)
                         resource-type (:type resource)]
                    (assoc acc [x y] [resource-type _])))
                 {} payload)]
     (assoc db :res data))))

(re-frame.core/reg-event-db
 ::init-player
 (fn [db [_ res]]
   (assoc-in db [:player :id] (:id res))))

(defonce ws
  #?(:cljs
     (if (= "aitem.github.io" js/window.location.host)
       (new js/WebSocket "wss://xmas.aidbox.dev/ws")
       (new js/WebSocket "ws://localhost:8080/ws")
       )
     :clj  nil))

(re-frame.core/reg-fx
 ::send
 (fn [data]
   (prn "Out: " data)
   (.send ws (str data))))

(set! (.. ws -onmessage)
      (fn [a]
        (let [response (clojure.edn/read-string (.-data a))]
          (prn "In: " (:event response))
          (case (:event response)
            "resources" (re-frame.core/dispatch [::save-resources (:data response)])
            "mines"     (re-frame.core/dispatch [::save-mines     (:data response)])
            "players"   (re-frame.core/dispatch [::save-players   (:data response)])
            "buildings" (re-frame.core/dispatch [::save-buildings (:data response)])
            "init" (re-frame.core/dispatch [::init-player (:data response)])
            nil))))
