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
   (assoc db :buildings buildings)))

(re-frame.core/reg-event-db
 ::save-resources
 (fn [db [_ res]]
   (assoc db :res res)))

(defonce ws
  #?(:cljs (new js/WebSocket "ws://localhost:8080/ws")
     :clj  nil))

(re-frame.core/reg-fx
 ::send
 (fn [data]
   (prn "SEND:" data)
   (.send ws (str data))))

(set! (.. ws -onmessage)
      (fn [a]
        (let [response (clojure.edn/read-string (.-data a))]
          (case (:event response)
            "resources" (re-frame.core/dispatch [::save-resources (:data response)])
            "mines"     (re-frame.core/dispatch [::save-mines     (:data response)])
            "players"   (re-frame.core/dispatch [::save-players   (:data response)])
            "buildings" (re-frame.core/dispatch [::save-buildings (:data response)])
            nil))))
