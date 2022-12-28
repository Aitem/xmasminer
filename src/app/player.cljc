(ns app.player
  (:require [re-frame.core :as rf]))

(defn get-viewport
  [db]
  (get db :viewport))

(defn update-viewport
  [viewport player-pos]
  (let [vp-x (:x viewport)
        vp-y (:y viewport)
        vp-w (:w viewport)
        vp-h (:h viewport)
        px (:x player-pos)
        py (:y player-pos)

        r-dist (- (+ vp-x vp-w) px)
        l-dist (- px vp-x)
        t-dist (- py vp-y)
        b-dist (- (+ vp-y vp-h) py)]
    (let [vp-dx (- (max 0 (- (+ 21 px) (dec (+ vp-x vp-w))))
                   (max 0 (- (+ 21 vp-x) px)))

          vp-dy (- (max 0 (- (+ 9 py) (dec (+ vp-y vp-h))))
                   (max 0 (- (+ 9 vp-y) py)))]
      {:x (+ vp-x vp-dx)
       :y (+ vp-y vp-dy)
       :h vp-h
       :w vp-w})))

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
