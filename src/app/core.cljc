(ns app.core
  (:require [reagent.dom :as rdom]
            [re-frame.core :as rf]
            [app.pages.core :as pages]
            [app.pages.index.model]
            [app.pages.index.view]
            [re-pressed.core :as rp]
            [zframes.srv :as srv]
            #?(:cljs [app.init])

            ;; Side-effectful only imports
            [app.ws]
            [app.player]))

(defn root []
  (let [m (rf/subscribe [app.pages.index.model/index-page])]
    (fn []
      (app.pages.index.view/view @m))))


(def fps 30)

(defn zoom-level->tile-size [level]
  (case level
    1 10
    2 20
    3 40
    4 80
    5 160
    nil 40))


(defn move-res [[pos [t o dx dy]] gmap tile-size]
  (let [d (/ tile-size fps)]
    (if-let [infra (get gmap pos)]
      (let [[building-type building-opts &_] infra]
        (condp = [building-type building-opts]
          [:b :r] {pos [t o (+ dx d) dy]}
          [:b :l] {pos [t o (- dx d) dy]}
          [:b :u] {pos [t o dx (- dy d)]}
          [:b :d] {pos [t o dx (+ dy d)]}

          {pos [t o]}
          ))
     {pos [t o]}
     ))
  )


(rf/reg-event-db
 ::animation
 (fn [db _]
   (let [gmap (:buildings db)
         zoom-level (:zoom-level db)
         tile-size (zoom-level->tile-size zoom-level)]
     (update db :res
             (fn [ress]
               (reduce (fn [acc r] (merge acc (move-res r gmap tile-size))) {} ress)
               )))
   ))

(rf/reg-fx
 :interval
 (let [live-intervals (atom {})]
   (fn [{:keys [action id frequency event]}]
     (if (= action :start)
       (swap! live-intervals assoc id (js/setInterval #(rf/dispatch event) frequency))
       (do (js/clearInterval (get @live-intervals id))
           (swap! live-intervals dissoc id))))))

(defn make-odd [n]
  (if (even? n)
    (inc n)
    n))


(rf/reg-event-db
 ::resize-viewport
 (fn [db [_ new-zoom-level]]
   (let [zoom-level (or new-zoom-level (:zoom-level db))
         tile-size (zoom-level->tile-size zoom-level)]
     (-> db
         (assoc-in [:viewport :w] (make-odd (inc (int (/ js/document.documentElement.clientWidth tile-size)))))
         (assoc-in [:viewport :h] (make-odd (inc (int (/ js/document.documentElement.clientHeight tile-size)))))))))


(rf/reg-event-fx
 ::init
 (fn [{db :db} _]
   (let [h (make-odd (inc (int (/ js/document.documentElement.clientHeight 40))))
         w (make-odd (inc (int (/ js/document.documentElement.clientWidth 40))))]

     {:fx [(when-not (:initialized db)
             [:interval {:action    :start
                         :id        :tick
                         :frequency (/ 1000 fps)
                         :event     [::animation]}])]
      :db (merge db {:initialized true
                     :zoom-level 3}
                 (when-not (:initialized db)
                   {:viewport {:x (- 0 (int (/ w 2)))
                               :y (- 0 (int (/ h 2)))
                               :h h
                               :w w}
                    :player {:position {:x 0 :y 0}}}))})))

(defn ^:dev/after-load init []
  (rf/dispatch [::init])
  (rdom/render [root] (.getElementById js/document "root")))
