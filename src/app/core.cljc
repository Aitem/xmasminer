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




(def fps 50)
(def d (/ 40 fps))

(defn move-res [[pos [t o dx dy]] gmap]
  (if-let [infra (get gmap pos)]
    (condp = infra
      [:b :r] {pos [t o (+ dx d) dy]}
      [:b :l] {pos [t o (- dx d) dy]}
      [:b :u] {pos [t o dx (- dy d)]}
      [:b :d] {pos [t o dx (+ dy d)]}

      {pos [t o]}
      )
    {pos [t o]}
    )
  )


(rf/reg-event-db
 ::animation
 (fn [db _]
   (let [gmap (:buildings db)]
     (update db :res
             (fn [ress]
               (reduce (fn [acc r] (merge acc (move-res r gmap))) {} ress)
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

(rf/reg-event-db
 ::resize-viewport
 (fn [db [_ w h]]
   (-> db
       (assoc-in [:viewport :h] h)
       (assoc-in [:viewport :w] w))))

(defn make-odd [n]
  (println "mo" n)
  (if (even? n)
    (inc n)
    n))

(rf/reg-event-fx
 ::init
 (fn [{db :db} _]
   (let [h (make-odd (inc (int (/ js/document.documentElement.clientHeight 40))))
         w (make-odd (inc (int (/ js/document.documentElement.clientWidth 40))))]
     {:fx [[:interval {:action    :start
                       :id        :tick
                       :frequency (/ 1000 fps)
                       :event     [::animation]}]]
      :db (merge db {:viewport {:x (- 0 (int (/ w 2)))
                                :y (- 0 (int (/ h 2)))
                                :h h
                                :w w}
                     :player {:position {:x 0 :y 0}}})})))

(defn ^:dev/after-load init []
  (rf/dispatch [::init])
  (rdom/render [root] (.getElementById js/document "root")))
