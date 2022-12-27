(ns app.pages.game.view
  (:require
   [zframes.pages :as pages]
   [reagent.core :as r]
   [re-frame.core :as rf]
   [re-dnd.events] ;; make sure the events are registered and inited properly.
   [re-dnd.views :as dndv] ;;make sure the internal components are registered.
   [re-dnd.subs] ;; make sure subs are registered
   [app.pages.game.model :as model]))

(defmethod dndv/dropped-widget :my-drop-marker [_] [:div.my-drop-marker ])

(defmethod dndv/dropped-widget
  :dropped-box
  [{:keys [type id]}]
  #_[:span.my-drop-marker ])

(defn stat-widget [s & [b]]
  [:div.stat-widget.flex
   [:span {:style {:margin-right "4px"}} "-"]
   [:div.stat-widget-itm {:class (when (= -2 s) (if b "blue" "red") )} ]
   [:div.stat-widget-itm {:class (case s -2 (if  b "blue" "red") -1 (if  b "blue" "yellow") "")}  ]
   [:div.stat-widget-itm {:class (case s -2 (if  b "blue" "red") -1 (if  b "blue" "yellow") -0 (if  b "blue" "green") 1 (if  b "blue" "yellow") 2 (if  b "blue" "red")  "")}  ]
   [:div.stat-widget-itm {:class (case s  2 (if  b "blue" "red")  1 (if  b "blue" "yellow") "")}  ]
   [:div.stat-widget-itm {:class (when (= 2 s) (if  b "blue" "red") )}  ]
   [:span {:style {:margin-left "4px"}} "+"]])


(defn drag-golden [_ _]
  (let [pt-state (rf/subscribe [::model/selected-pt])]
    (fn [pt obs]
      (let [o (group-by #(get-in % [:code :coding 0 :code]) obs)
            death? (get-in pt [:deceased :boolean])
            d death?]
        [:div.rpgui-container.pos-initial.drag.p-8.pt.hoverable.rpgui-cursor-point
        {:on-click #(rf/dispatch [::model/select-pt pt obs])
         :class (if death?
                  "framed"
                  (if (= (:id pt) (:id (:pt @pt-state)))
                    "framed-golden-2"
                    "framed-golden"))}
        [:div.flex.pt-10
         [:img.pt-monitor (if death?
                            {:src "./img/flatline.gif"}
                            {:src "./img/heartbeat.gif"})]
         [:div
          [:h3 {:style {:margin "0", :margin-bottom "5px"}} (get-in pt [:name 0 :given 0])]
          [:span
           [:span.pt-hp
            (for [i (range (:health pt))] ^{:key i}
              [:img.pt-icn {:src "./img/heart.png"}])
            (for [i (range (- 3 (:health pt)))] ^{:key i}
              [:img.pt-icn {:src "./img/heart_black.png"}])]
           [:span.pt-mn.p-8 [:img.pt-icn {:src "./img/coin_gold.png"}]
            (:balance pt)]]]]
        [:div.pt-stats
         (let [m (get-in (get o "temperature") [0 :value :Quantity :value])]
           [:div.flex.stat-row [:img.pt-icn.stat-icon {:src "./img/thermometer.png"}] [stat-widget m]])

         (let [m (get-in (get o "pressure")    [0 :value :Quantity :value])]
           [:div.flex.stat-row [:img.pt-icn.stat-icon {:src "./img/tonometer.png"}] [stat-widget m ]])


         (let [m (get-in (get o "sugar")    [0 :value :Quantity :value])]
           [:div.flex.stat-row [:img.pt-icn.stat-icon {:src "./img/sugar.png"}] [stat-widget m]])


         (let [m (get-in (get o "bacteria")    [0 :value :Quantity :value])]
           [:div.flex.stat-row [:img.pt-icn.stat-icon {:src "./img/bacteria.png"}] [stat-widget m ]])

         (let [m (get-in (get o "diarrhea")    [0 :value :Quantity :value])]
           [:div.flex.stat-row [:img.pt-icn.stat-icon {:src "./img/diarrhea.png"}] [stat-widget m]])

         ]]))))

(defn plusify [t] (if (> t 0 ) (str "+" t) t))


(defn drug [id m]
  (let [pt (rf/subscribe [::model/selected-pt])
        ds (rf/subscribe [::model/selected-drug])]
    (fn [id m]
      [:div.ib
       [:div.rpgui-container.framed.pos-initial.rpgui-cursor-grab-open.drag.rpgui-draggable
        {:on-click #(rf/dispatch [::model/select-drug m])}
        [:h3 (:name m)]
        [:div.flex
         [:div.rpgui-icon.empty-slot.tabl-ico
          [:img.med-img {:src (:image m)}]]
         [:div.grow-1
          [:div.grid-grug
           [:div {:style {:margin-bottom "5px"}}
            [:span [:img.pt-icn {:src "./dist/img/radio-on.png"}] (:action-point m)]]
           [:div {:style {:margin-bottom "5px"}}
            [:span [:img.pt-icn {:src "./img/coin_gold.png"}]     (:price m)]]

           (when-let [t (get-in m [:effects :temperature])]
             [:div [:span [:img.pt-icn {:src "./img/thermometer.png"}] (plusify t)]])
           (when-let [t (get-in m [:effects :pressure])]
             [:div [:span [:img.pt-icn {:src "./img/tonometer.png"}] (plusify t)]])
           (when-let [t (get-in m [:effects :sugar])]
             [:div [:span [:img.pt-icn {:src "./img/sugar.png"}] (plusify t)]])
           (when-let [t (get-in m [:effects :bacteria])]
             [:div [:span [:img.pt-icn {:src "./img/bacteria.png"}] (plusify t)]])
           (when-let [t (get-in m [:effects :diarrhea])]
             [:div [:span [:img.pt-icn {:src "./img/diarrhea.png"}] (plusify t)]])]]]

        (when (= (:id m) (:id @ds))
          (let [stats   (group-by #(get-in % [:code :coding 0 :code]) (:obs @pt))
                stats   (reduce-kv (fn [acc k v]
                                     (assoc acc (keyword k) (get-in v [0 :value :Quantity :value])))
                                   {} stats)
                res  (model/apply-stats stats (:effects m))]
           [:div
            [:hr]
            [:p {:style {:text-align "center" :margin 0 :font-size "14px"}} "Эффект"]
            [:div.pt-stats
             [:div.flex.stat-row [:img.pt-icn.stat-icon {:src "./img/thermometer.png"}] [stat-widget (:temperature res) ]]
             [:div.flex.stat-row [:img.pt-icn.stat-icon {:src "./img/tonometer.png"}]   [stat-widget (:pressure res)    ]]
             [:div.flex.stat-row [:img.pt-icn.stat-icon {:src "./img/sugar.png"}]       [stat-widget (:sugar res)       ]]
             [:div.flex.stat-row [:img.pt-icn.stat-icon {:src "./img/bacteria.png"}]    [stat-widget (:bacteria res)    ]]
             [:div.flex.stat-row [:img.pt-icn.stat-icon {:src "./img/diarrhea.png"}]    [stat-widget (:diarrhea res)    ]]]
            [:button.rpgui-button
             {:on-click #(rf/dispatch [::model/apply-drug (:pt @pt) m])
              :style {:width "100%"}}
             [:span "Применить"]]

            ]))]])))

(defonce state (r/atom {:selected :temperature}))


(defn aidbox [_ _]
  (let [set-fltr #(swap! state assoc :selected %)
        pt (rf/subscribe [::model/selected-pt])]
    (fn [med {:keys [total current]:as ap}]
      [:div.rpgui-container.framed-golden.pos-initial
       [:div.aidbox
        (let [mmeds (reduce-kv
                     (fn [acc k v]
                       (if (and (get-in v [:effects (:selected @state)])
                                (>= current (:action-point v)))
                         (assoc acc k v)
                         acc))
                     {} med)
              mmeds  (sort-by  (juxt
                                ;;#(get-in (val %) [:action-point])
                                #(get-in (val %) [:effects (:selected @state)])
                                )  mmeds)]
          (if (empty? mmeds)
            [:div.rpgui-container.framed.pos-initial.rpgui-cursor-grab-open.drag.rpgui-draggable
             [:div {:style {:margin-bottom "5px" :color "white"}}
              [:div "No action points left!" ]]]
            (into
             [:<>]
             (for [[id res] mmeds]  ^{:key id}
               [drug id res]))))]
       [:div.tab
        [:img.pt-icn.rpgui-cursor-point.img-img-img
         {:on-click #(set-fltr :temperature)
          :title "Температура"
          :src "./img/thermometer.png"
          :class (when (= :temperature (:selected @state)) "active")}]
        [:img.pt-icn.rpgui-cursor-point.img-img-img {:on-click #(set-fltr :pressure)
                                                     :title "Кровяное давление"
                                                     :src "./img/tonometer.png"
                                                     :class (when (= :pressure    (:selected @state)) "active")}]
        [:img.pt-icn.rpgui-cursor-point.img-img-img {:on-click #(set-fltr :sugar)
                                                     :title "Уровень сахара в крови"
                                                     :src "./img/sugar.png"
                                                     :class (when (= :sugar       (:selected @state)) "active")}]
        [:img.pt-icn.rpgui-cursor-point.img-img-img {:on-click #(set-fltr :bacteria)
                                                     :title "Бактерии"
                                                     :src "./img/bacteria.png"
                                                     :class (when (= :bacteria    (:selected @state)) "active")}]
        [:img.pt-icn.rpgui-cursor-point.img-img-img {:on-click #(set-fltr :diarrhea)
                                                     :title "Расстройства ЖКТ"
                                                     :src "./img/diarrhea.png"
                                                     :class (when (= :diarrhea    (:selected @state)) "active")}]]

       [:hr]
       [:div.apps
        (into [:<>]
              (for [a (repeat current "x")]
                [:img  {:width "20px" :src "./dist/img/radio-on.png"}]))
        (into [:<>]
              (for [a (repeat (- total current) "x")]
                [:img {:style {:filter "grayscale(100%)"}
                       :width "20px" :src "./dist/img/radio-on.png"}]))

        (into [:<>]
              (for [a (repeat (- 10 total) "x")]
                [:img.dsbl {:width "20px" :src "./dist/img/radio-off.png"}]))]

       [:hr]


       ])))


(defn koika-1 [patient]
  [:div.pt-koika
   [:img.a.x4.tumba   {:src "./img/tumba.png"}]
   [:img.a.x3.patient {:src (str "./img/" (or (:avatar patient) "patient.png"))
                  :class (if (get-in patient [:deceased :boolean]) "deceased" "alive")}]
   [:img.a.x4.koika   {:src "./img/koika.png"}]
   [:img.a.x4.wall    {:src "./img/wall.png"}]

   ])

(defn koika-2 [patient]
  [:div.pt-koika
   [:img.a.x4.blood   {:src "./img/blood.png"}]
   [:img.a.x3.patient {:src (str "./img/" (or (:avatar patient) "patient.png"))
                       :class (if (get-in patient [:deceased :boolean]) "deceased" "alive")}]
   [:img.a.x4.koika   {:src "./img/koika.png"}]
   [:img.a.x4.tumba   {:src "./img/tumba-cvet.png"}]
   [:img.a.x4.wall    {:src "./img/wall.png"}]
   ])

(defn koika-3 [patient]
  [:div.pt-koika
   [:img.a.x4.stul   {:src "./img/blood-taburet.png"}]
   ;;[:img.a.x4.tumba   {:src "./img/tumba.png"}]
   [:img.a.x3.patient {:src (str "./img/" (or (:avatar patient) "patient.png"))
                       :class (if (get-in patient [:deceased :boolean]) "deceased" "alive")}]
   [:img.a.x4.koika   {:src "./img/koika.png"}]])

(defn koika [idx patient] (let [k (case idx 0 koika-1 1 koika-2 koika-3)] [k patient]))

(pages/reg-subs-page
 model/index-page
 (fn [{dv :d pts :pts medics :aidbox obs :obs ap :ap :as  page} _]
   [:div.rpgui-container.framed.relative

    [:div.fsgrid
     [:div#g-patients {:style {:margin-right "15px" :margin-bottom "20px"}}
      [:div.relative.game {:style { :margin-bottom "20px"}}
       [:div.top-left-bordur] [:div.top-bordur] [:div.top-right-bordur]
       [:div.bottom-left-bordur] [:div.bottom-bordur] [:div.bottom-right-bordur]
       [:div.left-bordur] [:div.right-bordur]
       [:div.top-wall] [:div.top-door] [:div.logo]
       [:img.a.x4.lab      {:src "./img/lab.png"}]
       ;;[:img.a.x3.ray      {:src "./img/ray.png"}]
       [:img.a.x4.prep-stool    {:src "./img/prep-stool.png"}]
       [:img.a.x4.aaidbox  {:src "./img/aidbox.png"}]
       [:img.a.x4.clock    {:src "./img/clock.png"}]
       [:img.a.x4.rupor    {:src "./img/rupor.png"}]
       [:img.a.x4.stol1    {:src "./img/stol1.png"}]
       ;; [:img.a.x4.bb1    {:src "./img/bb1.png"}]

       ;; [:div.left-racovina] [:div.lab] [:div.wall-blood]

       [:div.flex.around.kik
        (for [[idx [k v]] (map-indexed vector pts)]
          [:div {:key idx :class (when (get-in v [:deceased :boolean]) "dsbl")}
           [koika idx v]])]]



      [:div.flex.around
       (for [[idx [k v]] (map-indexed vector pts)] ^{:key idx}
         [:div {:class (when (get-in v [:deceased :boolean]) "dsbl")}
          [drag-golden v (get obs k)]])]]

     [:div#g-aidbox
      [aidbox medics ap]]

     [:div#g-progress
      [:div.flex.ac
       [:div.grow-1.mr-3
        [:div
         [:div.rpgui-progress.blue {:data-rpguitype "progress"}
          [:div.rpgui-progress-track
           [:div.rpgui-progress-fill.blue
            {:style {:width (str (/ (* 100 (or (:game-step page) 1) ) 10) "%")}}]]
          [:div.rpgui-progress-left-edge]
          [:div.rpgui-progress-right-edge]]]]

       [:button.rpgui-button.golden
        {:style {:padding-top "0px" :width "370px"}
         :on-click #(rf/dispatch [::model/next-step])}
        (let [all-patients-died (= 3 (count (filter (fn [[k p]] (get-in p [:deceased :boolean])) pts)))]
          (cond (= 10 (:game-step page)) [:p {:style {:padding-top "5px"}} "Конец " (:game-step page) "/10"]
                all-patients-died  [:p {:style {:padding-top "5px"}} "Конец " (:game-step page) "/10"]
                :else [:p {:style {:padding-top "5px"}} "Следующий день " (:game-step page) "/10"]))]]]]]))
