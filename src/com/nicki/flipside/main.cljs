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
         :character {:c nil :r nil}
         :pathway []}))

(defn move-character
  "move the character to the next tile in the pathway"
  [app]
  (-> app
      (assoc-in [:character :c] (first (nth (:pathway app) 0)))
      (assoc-in [:character :r] (second (nth (:pathway app) 0)))))

(defn drop-first-tile
  "drop the first tile from the pathway vector"
  [app]
  (update-in app [:pathway] #(vec (rest %))))

(defn add-to-pathway
  [app c r]
  (if (= [c r] (last (butlast (:pathway app))))

    ;;remove the latest / last tile in the pathway
    (update-in app [:pathway] pop)

    ;;add the new tile to the end of the pathway vector
    (update-in app [:pathway] conj [c r])))

(defn tick
  [app]
  "move the character and drop the first tile from the pathway"
  (if (< 1 (count (:pathway app)))
    (-> app
        (move-character)
        (drop-first-tile))
    app))


(defn trigger!
  [event]
  (swap! !app (fn [app]
                (match [event]

                       [{:event/tick nil}]
                       (tick app)

                       [{:event/hover-tile {:c c :r r }}]
                       (add-to-pathway app c r)))))


(defn draw-grid
  [columns rows box-size app]
  (for [c (range 1 (inc columns))
        r (range 1 (inc rows))]
    [:.grid-box {:style {:width (str box-size "px")
                         :height (str box-size "px")
                         :-webkit-transform (str "translate3d(" (* box-size c) "px, "
                                                 (* box-size r) "px, 0px)")}
                 :id (str c "-" r)
                 :on-mouse-over (fn []
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
;;    (swap! !app conj {:fun 23})
    
    ;;step forward regularly
    (js/setInterval #(trigger! {:event/tick nil}) 600)
    ))

(defn adjacent-tiles?
  "given two tiles in the grid, check if they share an edge"
  [vec1 vec2]
  (let [[c1 r1] vec1
        [c2 r2] vec2]
    ;;first, make sure the two arguments are not the same
    (if (not= [c1 r1] [c2 r2])
      ;;now, check to see if the tiles share a row or column
      (cond

        ;; if the tiles share a column, the rows should be next to each other
        (and (= c1 c2)
             (<= -1 (- r1 r2) 1))
        true

        ;; if the tiles share a row, the columns should be next to each other
        (and (= r1 r2)
             (<= -1 (- c1 c2) 1))
        true

        :else
        false)
      false)))

;;;;;;Tests;;;;;;;

;; a tile can be added to an empty pathway
(let [fake-app {:pathway []}]
  (when-not (-> fake-app
                ;;add the first tile to the pathway
                (add-to-pathway ,, 1 1)
                ;;check that the tile was added
                (:pathway ,,)
                (= ,, [[1 1]]))
    (p "Test failed: a tile can't be added to an empty pathway")))

;; a tile can be added to a non-empty pathway
(let [fake-app {:pathway []}]
  (when-not (-> fake-app
                ;;add the first tile to the pathway
                (add-to-pathway ,, 1 1)
                ;;add a second tile to the pathway
                (add-to-pathway ,, 2 1)
                ;;check that the second tile was added
                (:pathway ,,)
                (= ,, [[1 1] [2 1]]))
    (p "Test failed: a tile can't be added to a non-empty pathway")))

;; when you add a tile to the pathway it should be adjacent (up/down/left/right) to the previous tile (if there are tiles in the pathway), there cannot be a gap in the pathway and you cannot move diagonally

;; check that you're not adding diagonal tiles
(let [fake-app {:pathway []}]
  (when-not (-> fake-app
                ;;add the first tile to the pathway
                (add-to-pathway ,, 1 1)
                ;;add a second tile to the pathway
                (add-to-pathway ,, 2 2)
                ;;check that the second tile wasn't added
                (:pathway ,,)
                (= ,, [[1 1]]))
    (p "Test failed: you're letting diagonal tiles be added to the pathway")))


;; check that you're not adding a tile that creates a gap in the pathway
(let [fake-app {:pathway []}]
  (when-not (-> fake-app
                ;;add the first tile to the pathway
                (add-to-pathway ,, 1 1)
                ;;add a second tile to the pathway
                (add-to-pathway ,, 3 2)
                ;;check that the second tile wasn't added
                (:pathway ,,)
                (= ,, [[1 1]]))
    (p "Test failed: you're letting gaps be created in the pathway")))

;; the character is always on a tile adjacent to the beginning of the pathway when the pathway is non-empty
(let [fake-app {:pathway [[1 1]]
                :character {:c nil :r nil}}
      character-pos (fn [map] (let [c (:c (:character map))
                                    r (:r (:character map))] [c r]))]
  (when-not (-> fake-app
                ;;add the first tile to the pathway
                (add-to-pathway ,, 1 2)
                ;;add a second tile to the pathway
                (add-to-pathway ,, 2 2)
                ;;run the tick function
                (tick ,,)
                ;;grab the character position
                (character-pos ,,)
                ;;check if the character is adjacent to the beginning of the pathway
                (adjacent-tiles? ,, [1 2]))
    (p "Test failed: your character might not be adjacent to the beginning of the pathway")))

;; you can only add a tile to the pathway when your pointer is located on the final tile of the pathway OR when there is no pathway and your pointer is located on the character's tile

;; if you move your pointer to the previous tile in the pathway OR to the character's tile, the tile you just left is removed from the pathway (retracing)


