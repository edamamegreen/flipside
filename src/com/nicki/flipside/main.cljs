(ns com.nicki.flipside.main
  (:require-macros [com.nicki.util.macros :refer [p pp]]
                   [lonocloud.synthread :as ->])
  (:require [cljs.core.match :refer-macros [match]]
            [datascript.core :as d]
            [rum.core :as rum]))

(enable-console-print!)

(def grid-box-size 50)
(def num-of-grid-rows 10)
(def num-of-grid-columns 10)

(defonce !app
  (atom {:play? false
         :character {}
         :pathway []}))


(defn trigger!
  [event]
  (swap! !app (fn [app]
                (match [event]

                       [{:event/hover-tile {:c c :r r }}]
                       (if (= [c r] (last (butlast (:pathway app))))
                         (update-in app [:pathway] pop)
                         (update-in app [:pathway] conj [c r]))))))


(defn draw-grid
  [columns rows box-size app]
  (for [c (range 1 (inc columns))
        r (range 1 (inc rows))]
    [:.grid-box {:style {:width (str box-size "px")
                         :height (str box-size "px")
                         :-webkit-transform (str "translate3d(" (* box-size c) "px, "
                                                 (* box-size r) "px, 0px)")}
                 :id (str c "-" r)

                 :on-mouse-over
                 (fn []
                   (trigger! {:event/hover-tile {:c c :r r }}))

                 :data-box-in-path (some #(= [c r] %) (:pathway app))}]))


(defn draw-character
  [box-size app]
  (let [c (first (first (:pathway app)))
        r (second (first (:pathway app)))]
  [:img {:src "content/banana.svg"
           :style {:width "40px"
                   :height "40px"
                   :-webkit-transform (str "translate3d(" (+ 5 (* box-size c)) "px, " (+ 5 (* box-size r)) "px, 0px)")}}]))


(rum/defc *app
  [trigger! app]

  [:.app

   (draw-grid num-of-grid-columns
              num-of-grid-rows
              grid-box-size
              app)

   (draw-character grid-box-size app)])


(defn render!
  [app]
  (.render js/ReactDOM (*app trigger! app)
           (.getElementById js/document "container"))
  (p (str (:pathway app))))


(defonce hack
  (do
    ;;re-render watcher
    (add-watch !app :re-render (fn [_ _ old new] (render! new)))

    ;;change something so that the app renders the first time
    (swap! !app assoc :play? true)))