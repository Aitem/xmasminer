(ns app.player
  (:require [re-frame.core :as rf]))

(defn get-viewport
  [db]
  (get db :viewport))

(defn update-viewport
  [viewport player-pos]
  (let [vp-w (:w viewport)
        vp-h (:h viewport)
        px (:x player-pos)
        py (:y player-pos)]
    {:x (- px (int (/ vp-w 2)))
     :y (- py (int (/ vp-h 2)))
     :h vp-h
     :w vp-w}))

(rf/reg-event-fx
 ::move-d
 (fn [{db :db} _]
   (let [new-db (update-in db [:player :position :x] inc)
         vp (get-viewport db)
         player-pos (get-in new-db [:player :position])
         new-wp (update-viewport vp player-pos)
         new-db (assoc new-db :viewport new-wp)]
     {:db new-db
      :app.ws/send {:event "move-x"
                    :data  (get-in new-db [:player :position :x])}})))


(rf/reg-event-fx
 ::move-a
 (fn [{db :db} _]
   (let [new-db (update-in db [:player :position :x] dec)
         vp (get-viewport db)
         player-pos (get-in new-db [:player :position])
         new-wp (update-viewport vp player-pos)
         new-db (assoc new-db :viewport new-wp)]
     {:db new-db
      :app.ws/send {:event "move-x"
                    :data  (get-in new-db [:player :position :x])}})))


(rf/reg-event-fx
 ::move-w
 (fn [{db :db} _]
   (let [new-db (update-in db [:player :position :y] dec)
         vp (get-viewport db)
         player-pos (get-in new-db [:player :position])
         new-wp (update-viewport vp player-pos)
         new-db (assoc new-db :viewport new-wp)]
     {:db new-db
      :app.ws/send {:event "move-y"
                    :data  (get-in new-db [:player :position :y])}})))

(rf/reg-event-fx
 ::move-s
 (fn [{db :db} _]
   (let [new-db (update-in db [:player :position :y] inc)
         vp (get-viewport db)
         player-pos (get-in new-db [:player :position])
         new-wp (update-viewport vp player-pos)
         new-db (assoc new-db :viewport new-wp)]
     {:db new-db
      :app.ws/send {:event "move-y"
                    :data  (get-in new-db [:player :position :y])}})))

(rf/reg-event-db
 ::clear
 (fn [db _]
   (dissoc db :buildings-menu-item)))

(rf/reg-event-fx
 ::zoom-smooth
 (fn [{db :db} [_ dy]]
   (println "smoothzoom")
   (println "smoothzoom dy" dy)
   (let [current-zoom-level (:zoom-level db)
         ;; in Firefox one level in 138 on my machine
         one-level-pixels 138
         new-zoom-level (/ (- (* one-level-pixels current-zoom-level) dy) one-level-pixels)
         new-zoom-level (cond (< new-zoom-level 1) 1
                              (> new-zoom-level 5) 5
                              :else new-zoom-level)]
     (println "smoothzoom newlevel" new-zoom-level)
     {:db (assoc db :zoom-level new-zoom-level)
      :dispatch [:app.core/resize-viewport new-zoom-level]})))
