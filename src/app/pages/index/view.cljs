(ns app.pages.index.view
  (:require [zframes.pages :as pages]
            [re-frame.core :as rf]
            [app.pages.index.model :as model]))

(defn menu
  []
  [:div {:style {:padding-top "20px" :position "absolute"}}
   [:label "Name "]
   [:input {:type "text" :on-change #(rf/dispatch [::model/change-name (.. % -target -value)])}]
   ])

(defn buildings-menu
  []
  (let [state @(rf/subscribe [::model/buildings-menu])]
    [:div#buildings-menu
     [:div#buildings-menu-items
      (for [item (:items state)]
        [:div.item {:on-click #(rf/dispatch [::model/select-buildings-menu-item item])
                    :key (hash item)}
         [:div {:id (:id item) :class (:class item)}]])]]))

(defn init-map
  [object]
  (when object
    (.addEventListener
     (js/document.getElementById "map")
     "contextmenu"
     (fn [event]
       (.preventDefault event)
       (rf/dispatch [::model/remove-building])))
    (js/document.addEventListener
     "keydown"
     (fn [event]
       (case (.-key event)
         "r" (rf/dispatch-sync [::model/seleted-building-rotate])
         nil)))
    (.addEventListener
     (js/document.getElementById "map")
     "mousemove"
     (fn [event]
       (let [map-object (.getBoundingClientRect (js/document.getElementById "map"))]
         (rf/dispatch [::model/map-cursor (- (.-x event) (.-x map-object)) (- (.-y event) (.-y map-object))])
         (when (= 1 (.-buttons event))
           (rf/dispatch [::model/create-seleted-building]))
         (when (= 2 (.-buttons event))
           (rf/dispatch [::model/remove-building]))
         )))
    (.addEventListener
     (js/document.getElementById "map")
     "click"
     (fn [event]
       (rf/dispatch [::model/create-seleted-building])))))


(defn tile-type [t]
  (condp = t
     :b "belt"
     :m "miner"
     :h "hub"
     :q "quest"
     :f "miner"
     t))

(defn building-tile [[type opts]]
  (let [t (tile-type type)]
    (str "t " t " " t "-" (last (str opts)))))

(defn add-content [block content]
  (cond-> block
    content (conj block content)))

(defn render-generic-box
  ([box-spec content]
   (let [box-width (:width box-spec)
         box-height (:height box-spec)
         vp-x-pos (:vp-x box-spec)
         vp-y-pos (:vp-y box-spec)]
     (-> [:div.box {:key (str "box-" vp-x-pos "-" vp-y-pos "-" box-width "-" box-height)
                    :style {:grid-column-start (inc vp-x-pos)
                            :grid-column-end (+ vp-x-pos box-width)
                            :grid-row-start (inc vp-y-pos)
                            :grid-row-end (+ vp-y-pos box-height)}}]
         (add-content content))))
  ([box-spec]
   (render-generic-box box-spec nil)))

(defn render-tile
  ([box-spec content]
   (render-generic-box box-spec
                       (add-content
                        [:div.tile {:class (:tile box-spec)}]
                        content)))
  ([box-spec]
   (render-tile box-spec nil)))

(defn render-colorbox
  ([box-spec content]
   (render-generic-box box-spec
                       (add-content
                        [:div.colorbox {:style {:background (:color box-spec)}}]
                        content)))
  ([box-spec]
   (render-colorbox box-spec nil)))

(defn render-player [player-data]
  (render-colorbox {:width 1
                    :height 1
                    :vp-x (:vp-x player-data)
                    :vp-y (:vp-y player-data)}
                   [:div.player {:style {:color "black"
                                         :position "absolute"
                                         :text-align "center"
                                         :margin-top "-20px"}}]))

(defn make-odd [n]
  (if (even? n)
    (inc n)
    n))

(defn zoom-level->tile-size [level]
  (case level
    1 10
    2 20
    3 40
    4 80
    5 160
    nil 40))

(defn overflow-w [viewport tile-size]
  (let [browser-width js/document.documentElement.clientWidth
        viewport-width (* (:w viewport) tile-size)]
    (- viewport-width browser-width)))

(defn overflow-h [viewport tile-size]
  (let [browser-height js/document.documentElement.clientHeight
        viewport-height (* (:h viewport) tile-size)]
    (- viewport-height browser-height)))


(defn view [{{pos :position pid :id} :player
             mines :mines
             buildings :buildings res :res belt :belt :as page}]
  (let [player (first (filter #(= pid (:id %)) (:players page)))
        vp-h (get-in page [:viewport :h])
        vp-w (get-in page [:viewport :w])
        p-x (get-in player [:position :x])
        p-y (get-in player [:position :y])
        vp-x (- p-x (int (/ vp-w 2)))
        vp-y (- p-y (int (/ vp-h 2)))
        viewport (:viewport page)
        zoom-level (:zoom-level page)
        tile-size (zoom-level->tile-size zoom-level)]
    [:div#screen {:style {:overflow "hidden"}}
     [menu]
     [buildings-menu]
     [:span#info (str pos)]
     [:div#map {:ref init-map
                :class (str "bg-scale-" zoom-level)
                :style {:grid (str "repeat(" (get-in page [:viewport :h])
                                   ", " tile-size "px) / repeat("(get-in page [:viewport :w]) ", " tile-size "px)")
                        :margin-left (str "-" (int (/ (overflow-w viewport tile-size) 2)) "px")
                        :margin-top (str "-" (int (/ (overflow-h viewport tile-size) 2)) "px")
                        :background-size (str tile-size "px")}}
      (for [p (:players page)
            :let [vp-px (- (get-in p [:position :x]) vp-x)
                  vp-py (- (get-in p [:position :y]) vp-y)]
            :when (and (>= vp-px 0)
                       (>= vp-py 0)
                       (< vp-px vp-w)
                       (< vp-py vp-h))]
        [:div#player {:key (str "p-" (hash p))
                      :class (str "skin-" (:skin p) " block-scale-" zoom-level " bg-scale-" zoom-level)
                      :style {:grid-column (inc vp-px)
                              :grid-row (inc vp-py)}}
         [:div {:style {:color "black"
                        :position "absolute"
                        :text-align "center"
                        :margin-top "-20px"}}
          (:name p)]])

      (when (and (:cursor page)
                 (:buildings-menu-item page))
        (let [item-id (-> page :buildings-menu-item :id)
              item-x  (-> page :cursor :x)
              item-y  (-> page :cursor :y)]
          (if (model/allow-building-create? page (:buildings-menu-item page))
            [:div {:class (conj [(:class (:buildings-menu-item page))
                                 (str "block-scale-" zoom-level " bg-scale-" zoom-level)]
                                (building-tile [(:id (:buildings-menu-item page))
                                                (:dir (:buildings-menu-item page))]))
                   :style {:opacity     0.7
                           :grid-column item-x
                           :grid-row  item-y}}]
            [:div {:class (conj [(:class (:buildings-menu-item page))
                                 (str "block-scale-" zoom-level " bg-scale-" zoom-level)]
                                (building-tile [(:id (:buildings-menu-item page))
                                                (:dir (:buildings-menu-item page))]))
                   :style {:opacity     0.4
                           :background "red"
                           :grid-column item-x
                           :grid-row  item-y}}])))







      ;; buildings
      [:<>
       (for [building buildings
             :let [[[x y] [type opts fab _ state]] building
                   vp-bx (- x vp-x)
                   vp-by (- y vp-y)]
             :when (and (>= vp-bx 0)
                        (>= vp-by 0)
                        (< vp-bx vp-w)
                        (< vp-by vp-h))]
         [:div {:key (str x "-" y "-" type "-" opts)
                :class [(str "block-scale-" zoom-level " bg-scale-" zoom-level) (building-tile [type opts])]
                :style {:grid-column (inc vp-bx)
                        :grid-row (inc vp-by)}}
          
          (when fab
            [:<>
             [:div {:key (hash fab)
                    :class (str (condp = (:input fab)
                                  :c "circuite"
                                  :w "chip"
                                  "wire")
                                " block-scale-" zoom-level
                                " bg-scale-"    zoom-level

                                )

                    :style {:grid-column (inc vp-bx) :grid-row (inc vp-by)}}]
             (:count fab)])
          (when state
            (:count state))])]

      ;; resources
      [:<>
       (for [resource res
             :let [[[x y] [type opts dx dy]] resource
                   vp-rx (- x vp-x)
                   vp-ry (- y vp-y)]
             :when (and (>= vp-rx 0)
                        (>= vp-ry 0)
                        (< vp-rx vp-w)
                        (< vp-ry vp-h))]
         [:div {:key (str "r-" x "-" y "-" type "-" dx "-" dy "1")

                :class (str (condp = type
                              :c "circuit"
                              :w "chip"
                              "wire")
                            " block-scale-" zoom-level
                            " bg-scale-"    zoom-level)

                :style {:margin-left (str (* 2 dx) "px")
                        :margin-top  (str (* 2 dy) "px")
                        :grid-column (inc vp-rx) :grid-row (inc vp-ry)}}
          ])]

      ;; mines
      [:<>
       (for [mine mines
             :let [[[x y] [type opts dx dy]] mine
                   vp-mx (- x vp-x)
                   vp-my (- y vp-y)]
             :when (and (>= vp-mx 0)
                        (>= vp-my 0)
                        (< vp-mx vp-w)
                        (< vp-my vp-h))]
         [:div {:key (str "m-" x "-" y "-" type)
                :class (str "mine mine-" (second (str type)) " block-scale-" zoom-level " bg-scale-" zoom-level)
                :style {:grid-column (inc vp-mx) :grid-row (inc vp-my)}}])]]]))
