(ns aergia.tcp-client
  (:require
   [aleph.tcp :as tcp]
   [clojure.edn :as edn]
   [gloss.core :as gloss]
   [gloss.io :as io]
   [manifold.deferred :as d]
   [clojure.data.json :as json]
   [manifold.stream :as s]))

(def protocol
  (gloss/compile-frame
   (gloss/finite-frame :uint32
                       (gloss/string :utf-8))
   pr-str
   edn/read-string))

(defn wrap-duplex-stream
  [protocol s]
  (let [out (s/stream)]
    (s/connect
     (s/map #(io/encode protocol %) out)
     s)

    (s/splice
     out
     (io/decode-stream s protocol))))

(defn client
  [host port]
  (d/chain (tcp/client {:host host, :port port})
           #(wrap-duplex-stream protocol %)))

(def header-struct [:p-version :uint16-le
                    :p-data-blk-num :uint32-le
                    :p-timestamp :uint64-le
                    :b-version :uint16-le
                    :b-type :uint16-le
                    :b-len :uint32-le
                    :c-type :uint32-le
                    :c-len :uint32-le
                    :json (gloss/string :utf-8)])

(def header-prot
  (gloss/compile-frame header-struct
                       pr-str
                       edn/read-string))

(defn test-client
  [host port]
  (d/chain (tcp/client {:host host, :port port})
           #(wrap-duplex-stream header-prot %)))

(def c @(test-client "localhost" 8082))


(def header-fmt (gloss/compile-frame header-struct))

(def header-val [:p-version 1
                 :p-data-blk-num 2
                 :p-timestamp 3
                 :b-version 4
                 :b-type 5
                 :b-len 6
                 :c-type 7
                 :c-len 8])

(def ack-struct {:p-version :uint16
                 :p-data-blk-num :uint32
                 :p-timestamp :uint64
                 :b-version :uint16
                 :b-type :uint16
                 :b-len :uint32
                 :ack :uint8})


@(s/put! c header-byte)
;;offline seq
;;send
(def req-offline-filesetting {:Behavior "REQ_Offline_FileSettings"
                              :LogFile "/home/hss/Downloads/logdata/pandarxt32.bin"
                              :CalibrationFile "/media/hss/log/cal.txt"})

(def c @(test-client "localhost" 8082))

(defn cmd-offline-filesetting [client]
  @(s/put! client [:p-version 1
                   :p-data-blk-num 1
                   :p-timestamp 0xeeeeeeeeeeeeeeee
                   :b-version 1
                   :b-type 4
                   :b-len 0
                   :c-type 5
                   :c-len 0
                   :json (json/write-str req-offline-filesetting)]))

(defn receive-ack [client]
  @(s/take! client))

(defn receive-rsp [client]
  @(s/take! client))

(defn offline-file-setting []
  (let [client @(test-client "localhost" 8082)]
    (cmd-offline-filesetting client)
    (receive-ack client)
    (receive-rsp client)))

;;Recv Ack

;;recv
(def rsp-offline-filesetting {:Behavior "RSP_Offline_Filesettings"
                              :Result 1
                              :Description "Success"})


;; send ack
