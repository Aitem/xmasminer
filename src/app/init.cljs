(ns app.init
  (:require [re-frame.core :as rf]))

(defonce initialize
  (do 
    (js/window.addEventListener
     "resize"
     (fn []
       (rf/dispatch [:app.core/resize-viewport])))
    (js/document.addEventListener
     "keydown"
     (fn [event]
       (case (.-key event)
         "Escape" (rf/dispatch-sync [:app.player/clear])
         "w" (rf/dispatch-sync [:app.player/move-w])
         "a" (rf/dispatch-sync [:app.player/move-a])
         "s" (rf/dispatch-sync [:app.player/move-s])
         "d" (rf/dispatch-sync [:app.player/move-d])
         nil)))
    (js/document.addEventListener
     "wheel"
     (fn [event]
       (.preventDefault event)
       (let [dy (.-deltaY event)]
         (rf/dispatch-sync [:app.player/zoom dy]))))))
