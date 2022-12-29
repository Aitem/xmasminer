(ns app.init
  (:require [re-frame.core :as rf]))

(defn make-odd [n]
  (if (even? n)
    (inc n)
    n))

(defonce initialize
  (do 
    (js/window.addEventListener
     "resize"
     (fn []
       (rf/dispatch [:app.core/resize-viewport
                     (make-odd (inc (int (/ js/document.documentElement.clientWidth 40))))
                     (make-odd (inc (int (/ js/document.documentElement.clientHeight 40))))])))
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
