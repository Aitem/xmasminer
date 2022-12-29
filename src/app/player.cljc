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
 ::zoom
 (fn [{db :db} [_ dy]]
   (let [current-zoom-level (:zoom-level db)
         down? (> dy 0)
         new-zoom-level (cond (and down? (> current-zoom-level 1)) (dec current-zoom-level)
                              (and (not down?) (< current-zoom-level 5)) (inc current-zoom-level)
                              :else current-zoom-level)]
     {:db (assoc db :zoom-level new-zoom-level)
      :dispatch [:app.core/resize-viewport new-zoom-level]}
     )))
