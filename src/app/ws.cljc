(ns app.ws
  (:require clojure.edn
            re-frame.core))

(re-frame.core/reg-event-db
 ::save-players
 (fn [db [_ players]]
   (assoc db :players players)))

(defonce ws
  #?(:cljs (new js/WebSocket "ws://localhost:8080/ws")
     :clj  nil))

(re-frame.core/reg-fx
 ::send
 (fn [data]
   (.send ws (str data))))

(set! (.. ws -onmessage)
      (fn [a]
        (re-frame.core/dispatch-sync
         [::save-players (:data (clojure.edn/read-string (.-data a)))])))
