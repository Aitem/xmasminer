(ns app.ws
  (:require clojure.edn
            re-frame.core))

(re-frame.core/reg-event-db
 ::save-players
 (fn [db [_ players]]
   (assoc db :players players)))

(re-frame.core/reg-event-db
 ::save-world (fn [db [_ w]] (assoc db :world w)))

(re-frame.core/reg-event-db
 ::save-world
 (fn [db [_ w]]
   (assoc db :world w )))

(re-frame.core/reg-event-db
 ::save-mines
 (fn [db [_ payload]]
   (let [data
         (reduce (fn [acc mine]
                   (let [x (:x mine)
                         y (:y mine)
                         _id (:id mine)
                         resource (:resource mine)]
                     (assoc acc [x y] [resource nil])))
                 {} payload)]
     (assoc db :mines data))))

(re-frame.core/reg-event-db
 ::save-buildings
 (fn [db [_ payload]]
   (let [data
         (reduce (fn [acc building]
                   (let [x (:x building)
                         y (:y building)
                         _id (:id building)
                         building-type (:type building)
                         opts (get-in building [:data :opts])
                         fab (get-in building [:data :fab])
                         state (get-in building [:data :state])]
                     (assoc acc [x y] [building-type opts fab nil state])))
                 {} payload)]
     (doseq [a (js/document.getAnimations)]
       (set! (.-startTime a) 0))

     (assoc db :buildings data))))

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
            "world"     (re-frame.core/dispatch [::save-world     (:data response)])
            "buildings" (re-frame.core/dispatch [::save-buildings (:data response)])
            "init" (re-frame.core/dispatch [::init-player (:data response)])
            nil))))
