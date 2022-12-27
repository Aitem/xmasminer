(ns app.init
  (:require [re-frame.core :as rf]))

(defonce initialize
  (do 
    (js/document.addEventListener
     "keydown"
     (fn [event]
       (case (.-key event)
         "w" (rf/dispatch-sync [:app.player/move-w])
         "a" (rf/dispatch-sync [:app.player/move-a])
         "s" (rf/dispatch-sync [:app.player/move-s])
         "d" (rf/dispatch-sync [:app.player/move-d])
         nil)))))
