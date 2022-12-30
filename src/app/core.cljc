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

(rf/reg-fx
 ::play-soundtrack
 (fn []
   (set! (.-volume app.init/soundtrack) 0.02)
   (.play app.init/soundtrack)))

(rf/reg-sub
 ::play?
 (fn [db _]
   (:play? db)))

(rf/reg-event-fx
 ::play
 (fn [{db :db} [_ value]]
   {::play-soundtrack {}
    :db (assoc db :play? true)}))

(defn mainmenu
  []
  (let [play? @(rf/subscribe [::play?])]
    (when-not play?
      [:dialog.nes-dialog.is-rounded {:open true :style {:margin-top "100px" :z-index 9999}}
       [:h1 {:style {:text-align "center"}} "XmasMiner"]
       [:p.nes-text.is-success {:style {:text-align "center" :margin-bottom "45px"}} "Mine the mood!"]
       [:label "Your name"]
       [:input.nes-input {:on-change #(rf/dispatch [:app.pages.index.model/change-name (.. % -target -value)])}]
       [:div {:style {:display "flex" :justify-content "center"}} 
        [:button.nes-btn.is-primary
         {:style {:margin "50px 0px 20px 0px" :padding "10px 40px"}
          :on-click #(rf/dispatch [::play])}
         "Play"]]])))

(defn root []
  (let [m (rf/subscribe [app.pages.index.model/index-page])]
    (fn []
      [:<> 
       [:div#fire {:style {:pointer-events "none" :position "absolute" :left 0 :right 0 :top 0 :bottom 0 :z-index 99999}}]
       [mainmenu]
       (app.pages.index.view/view @m)])))


(def fps 20)

(defn zoom-level->tile-size [level]
  (if level
    (let [scale-factor (js/Math.pow 2 (- level 3))
          tile-size (int (* scale-factor 40))]
      tile-size)
    40))


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
