(ns cljspazzer.client.player
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [cljspazzer.client.utils :as utils]
            [cljspazzer.client.channels :as channels]
            [cljspazzer.client.audio :refer [audio-node]]
            [cljspazzer.client.views.albums :as albums]
            [cljspazzer.client.views.artists :as artists]
            [cljspazzer.client.views.tracks :as tracks]
            [cljspazzer.client.views.nav :as nav]
            [cljs.core.async :refer [<! put! chan pub]]
            [sablono.core :as html :refer-macros [html]]
            ))

(defn ctrl-audio-node [action]
  (let [seek-offset 5]
    (case action
      :stop (do (.pause audio-node)
                (aset audio-node "currentTime" 0))
      :play (.play audio-node)
      :pause (.pause audio-node)
      :seek-backward (aset audio-node "currentTime" (- (.-currentTime audio-node) 1))
      :seek-forward (aset audio-node "currentTime" (+ (.-currentTime audio-node) 1))
      (.pause audio-node))))

(defn audio-elem [data owner]
  (reify
    om/IInitState
    (init-state [this]
      {:current-src ""
       :current-offset 0
       :ctrl :stop
       :track-src []
       :current-position 0
       :end-position 0})
    om/IWillMount
    (will-mount [this]
      (let [set-track (fn [track offset]
                        (om/set-state! owner :current-offset offset)
                        (aset audio-node "src" (tracks/mk-track-url track))
                        (ctrl-audio-node :play)
                        (put! channels/now-playing track))]
        (go (loop []
              (let [track-src (<! channels/track-list)]
                (set-track (first track-src) 1)
                (om/set-state! owner :current-src track-src))
              (recur)))
        (go (loop []
              (let [ctrl (<! channels/player-ctrl)
                    current-offset (or (om/get-state owner :current-offset) 0)
                    previous-offset (dec current-offset)
                    next-offset (inc current-offset)
                    track-src (om/get-state owner :current-src)
                    next-track (last (take next-offset track-src))
                    previous-track (last (take previous-offset track-src))
                    ]
                (om/set-state! owner :ctrl ctrl)
                (cond
                  (and (= ctrl :next) (> next-offset (count track-src)))
                  (ctrl-audio-node :stop)
                  (and (= ctrl :next) (not (nil? next-track)))
                  (set-track next-track next-offset)
                  (and (= ctrl :previous) (not (nil? previous-track)))
                  (set-track previous-track previous-offset)
                  :else (ctrl-audio-node ctrl)))
              (recur)))
        (go (loop []
              (<! channels/stream-position)
              (let [c (.-currentTime audio-node)
                    e (.-duration audio-node)]
                (if (= 0 c)
                  (do
                    (om/set-state! owner :current-position 0)
                    (om/set-state! owner :end-position 0))
                  (do
                    (om/set-state! owner :current-position c)
                    (om/set-state! owner :end-position e))))
              (recur)))
        ))
    om/IRender
    (render [this]
      (let [ctrl-current (om/get-state owner :ctrl)
            is-playing? (not (.-paused audio-node))
            current-position (om/get-state owner :current-position)
            end-position (om/get-state owner :end-position)]
        (html [:div
               [:progress {:max end-position :value current-position}]
               [:div (utils/format "%s/%s"
                                   (utils/format-duration current-position)
                                   (utils/format-duration end-position))]
               [:ul
                [:li {:on-click (fn [e] (put! channels/player-ctrl :previous))}
                 [:i.fa.fa-fast-backward]]
                [:li {:on-click (fn [e] (put! channels/player-ctrl :seek-backward))}
                 [:i.fa.fa-step-backward]]
                [:li {:on-click (fn [e] (put! channels/player-ctrl (if is-playing? :pause :play)))}
                 (if is-playing?
                   [:i.fa.fa-pause]
                   [:i.fa.fa-play])]
                [:li {:on-click (fn [e] (put! channels/player-ctrl :stop))}
                 [:i.fa.fa-stop]]
                [:li {:on-click (fn [e] (put! channels/player-ctrl :seek-forward))}
                 [:i.fa.fa-step-forward]]
                [:li {:on-click (fn [e] (put! channels/player-ctrl :next))}
                 [:i.fa.fa-fast-forward]]
                ]])))))

(defn view-now-playing [data owner]
  (reify
    om/IInitState
    (init-state [this]
      {:now-playing {"track" {}}})
    om/IWillMount
    (will-mount [this]
      (go
        (loop []
          (let [current-track (<! channels/now-playing)]
            (om/set-state! owner :now-playing current-track))
          (recur))))
    om/IRender
    (render [this]
      (let [current-track (om/get-state owner :now-playing)
            t (current-track "track")
            artist (t "artist_canonical")
            album (t "album_canonical")
            title (t "title_canonical")
            track-num (t "track")
            year (t "year")
            artist-nav (utils/format "#/artists/%s" (utils/encode artist))
            album-nav (utils/format "%s/albums/%s" artist-nav (utils/encode album))

            album-image (albums/mk-album-image artist t)
            artist-image (artists/mk-artist-image artist true)
            track-heading (utils/format "%s - %s (%s)" artist title year)]
        (html (if (not (nil? artist))
                [:div
                 [:a {:href album-nav}
                  [:img {:src album-image}]]
                 [:span track-heading]
               ]))))))
