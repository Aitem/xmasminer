(ns app.ws
  (:require clojure.edn
            re-frame.core))

(re-frame.core/reg-event-db
 ::save-players
 (fn [db [_ players]]
   (assoc db :players players)))

(re-frame.core/reg-event-db
 ::save-buildings
 (fn [db [_ buildings]]
   (assoc db :buildings buildings)))

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
            "players"   (re-frame.core/dispatch-sync [::save-players   (:data response)])
            "buildings" (re-frame.core/dispatch-sync [::save-buildings (:data response)])
            nil))))
