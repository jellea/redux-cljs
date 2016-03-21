(ns spaghetti.webaudio)

(defonce ctx (js/AudioContext.))
;(defonce midi (js/WebMIDIAPIWrapper. true))

(defonce node-types
  {:OscillatorNode {:transit-tag "wa-oscillator"
                    :mount-fn (fn [node] (.start node))
                    :create-fn #(.createOscillator ctx)
                    :unmount-fn (fn [node] (.stop node))
                    :io [{:dir :input :n :type :type :choices :choices ["sine" "triangle" "sawtooth" "square"] :default "sine"}
                         {:dir :input :n :frequency :type :number :default 440}
                         {:dir :input :n :detune :type :number :default 0}
                         {:dir :output :n :output}]}

   :MidiNode {:transit-tag "wm-midi"
              :create-fn #(js/WebMIDIAPIWrapper.)
              :io [{:dir :input :n :channel :type :number :default 1}
                   {:dir :input :n :type :type :choices :choices ["noteon" "noteoff" "poly aftertouch"]
                    :default "noteon"}
                   {:dir :output}]}

   :GainNode {:transit-tag "wa-gain"
              :create-fn #(.createGain ctx)
              :io [{:dir :input :n :input}
                   {:dir :input :n :gain :type :number :default 1}
                   {:dir :output :n :output}]}

   :DelayNode {:create-fn #(.createDelay ctx)
               :io [{:dir :input :n :input}
                    {:dir :input :n :delayTime :type :number :default 0}
                    {:dir :output :n :output}]}

   ;  :AudioBufferSourceNode {:create-fn #(.createBuffer ctx) :io [:playbackRate :loop :loopStart :loopEnd :buffer]}
   ;  :PannerNode {:create-fn #(.createPanner ctx) :io [:input :panningModel :distanceModel :refDistance :maxDistance :rolloffFactor :coneInnerAngle :coneOuterAngle :coneOuterGain]}
   ;  :ConvolverNode {:create-fn #(.createConvolver ctx) :io [:input :buffer :normalize]}
   :DynamicsCompressorNode {:create-fn #(.createDynamicsCompressor ctx)
                            :io [{:dir :input :n :input}
                                 :input :threshold :knee :ratio :reduction :attack :release]}

   :BiquadFilterNode {:transit-tag "wa-biquadfilter"
                      :create-fn #(.createBiquadFilter ctx)
                      :io [{:dir :input :n :input}
                           {:dir :input :n :type :type :choices
                            :choices ["lowpass" "highpass" "bandpass" "lowshelf"
                                      "highshelf" "peaking" "notch" "allpass"]
                            :default "lowpass"}
                           {:dir :input :n :frequency :type :number :default 350}
                           {:dir :input :n :Q :type :number :default 1}
                           {:dir :input :n :detune :type :number :default 0}
                           {:dir :input :n :gain :type :number :default 0}
                           {:dir :output :n :output}]}

   :WaveShaperNode {:transit-tag "wa-waveshaper"
                    :create-fn #(.createWaveShaper ctx)
                    :io [{:dir :input :n :input}
                         {:dir :input :n :curve :type :number}
                         {:dir :input :n :oversample :type :number}
                         {:dir :output :n :output}]}

   :AudioDestinationNode {:transit-tag "wa-destination"
                          :mount-fn (fn [node] (.connect node (.-destination ctx)))
                          :create-fn #(.createGain ctx)
                          :io [{:dir :input :n :input}]}

   :ChannelSplitterNode {:transit-tag "wa-splitter"
                         :create-fn #(.createChannelSplitter ctx)
                         :io [{:dir :input :n :input}
                              {:dir :output :n :output1}
                              {:dir :output :n :output2}]}

   :ChannelMergerNode {:transit-tag "wa-merger"
                       :create-fn #(.createChannelMerger ctx)
                       :io [{:dir :input :n :input1}
                            {:dir :input :n :input2}]}})


;; TRANSIT READERS + WRITERS

(defn gain-read-handler [x]
  (.createGain ctx))

(defn filter-read-handler [x]
  (.createBiquadFilter ctx))

(defn oscil-read-handler [x]
  (.createOscillator ctx))

(defn delay-read-handler [x]
  (.createDelay ctx))

(def audio-read-handlers
  {"gain" gain-read-handler
   "filter" filter-read-handler
   "delay" delay-read-handler
   "oscillator" oscil-read-handler})

(deftype ^:no-doc GainNodeHandler []
  Object
  (tag [_ v] "gain")
  (rep [_ v] "gain")
  (stringRep [this v] nil))

(deftype ^:no-doc DelayNodeHandler []
  Object
  (tag [_ v] "delay")
  (rep [_ v] "delay")
  (stringRep [this v] nil))

(deftype ^:no-doc FilterNodeHandler []
  Object
  (tag [_ v] "filter")
  (rep [_ v] "filter")
  (stringRep [this v] nil))

(deftype ^:no-doc OscillatorNodeHandler []
  Object
  (tag [_ v] "oscillator")
  (rep [_ v] "oscillator")
  (stringRep [this v] nil))

(def audio-write-handlers {(type (.createOscillator ctx)) (OscillatorNodeHandler.)
                           (type (.createBiquadFilter ctx)) (FilterNodeHandler.)
                           (type (.createDelay ctx)) (DelayNodeHandler.)
                           (type (.createGain ctx)) (GainNodeHandler.)})
