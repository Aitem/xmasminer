(ns app.init
  (:require [re-frame.core :as rf]))

(defonce soundtrack
  (js/Audio. "audio/soundtrack.mp3"))

(defonce initialize
  (do 
    (js/window.addEventListener
     "resize"
     (fn []
       (rf/dispatch [:app.core/resize-viewport])))
    (js/document.addEventListener
     "keydown"
     (fn [event]
       (case (.-keyCode event)
         27 (rf/dispatch-sync [:app.player/clear])
         87 (rf/dispatch-sync [:app.player/move-w])
         38 (rf/dispatch-sync [:app.player/move-w])
         65 (rf/dispatch-sync [:app.player/move-a])
         37 (rf/dispatch-sync [:app.player/move-a])
         83 (rf/dispatch-sync [:app.player/move-s])
         40 (rf/dispatch-sync [:app.player/move-s])
         68 (rf/dispatch-sync [:app.player/move-d])
         39 (rf/dispatch-sync [:app.player/move-d])
         nil)))
    (js/document.addEventListener
     "wheel"
     (fn [event]
       (.preventDefault event)
       (let [dy (.-deltaY event)]
         (rf/dispatch-sync [:app.player/zoom-smooth dy]))))))
