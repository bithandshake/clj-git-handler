
(ns git.side-effects
    (:require [candy.api  :refer [return]]
              [git.config :as config]
              [io.api     :as io]
              [string.api :as string]))

;; ----------------------------------------------------------------------------
;; ----------------------------------------------------------------------------

(defn get-gitignore
  ; @param (map)(opt) options
  ;  {:filepath (string)(opt)
  ;    Default: ".gitignore"}
  ;
  ; @usage
  ; (get-gitignore)
  ;
  ; @usage
  ; (get-gitignore {:filepath "my-directory/.gitignore"})
  ;
  ; @return (string)
  ([]
   (get-gitignore {}))

  ([{:keys [filepath] :or {filepath config/DEFAULT-GITIGNORE-FILEPATH}}]
   (io/read-file filepath)))

(defn ignored?
  ; @param (string) pattern
  ; @param (map)(opt) options
  ;  {:filepath (string)(opt)
  ;    Default: ".gitignore"}
  ;
  ; @usage
  ; (ignored? "my-file.ext")
  ;
  ; @usage
  ; (ignored? "my-file.ext" {:filepath "my-directory/.gitignore"})
  ;
  ; @return (boolean)
  ([pattern]
   (ignored? pattern {}))

  ([pattern options]
   (string/contains-part? (get-gitignore options)
                          (str "\n"pattern"\n"))))

(defn ignore!
  ; @param (string) pattern
  ; @param (map)(opt) options
  ;  {:block (string)(opt)
  ;    Default: "git-api"
  ;   :filepath (string)(opt)
  ;    Default: ".gitignore"}
  ;
  ; @usage
  ; (ignore! "my-file.ext")
  ;
  ; @usage
  ; (ignore! "my-file.ext" {:block "My ignored files"})
  ;
  ; @usage
  ; (ignore! "my-file.ext" {:filepath "my-directory/.gitignore"})
  ;
  ; @example
  ; (ignore! "my-file.ext" {:block "My ignored files"})
  ; =>
  ; "\n# My ignored files\nmy-file.ext\n"
  ;
  ; @return (string)
  ; Returns with the updated .gitignore file's content.
  ([pattern]
   (ignore! pattern {}))

  ([pattern {:keys [block] :or {block "git-api"} :as options}]
   (let [gitignore (get-gitignore options)]
        (letfn [(block-exists?    [block]     (string/contains-part? gitignore (str "# "block)))
                (write-gitignore! [gitignore] (println (str "git.api adding pattern to .gitignore: \""pattern"\""))
                                              (io/write-file! ".gitignore" gitignore {:create? true})
                                              (return gitignore))]
               (cond (ignored?      pattern options)
                     (return        gitignore)
                     (block-exists? block)
                     (let [gitignore (str (string/to-first-occurence gitignore (str "# "block))
                                          (str "\n"pattern)
                                          (string/after-first-occurence gitignore (str "# "block)))]
                          (write-gitignore! gitignore))
                     :else
                     (let [gitignore (str (string/ends-with! gitignore "\n")
                                          (str "\n# "block"\n"pattern"\n"))]
                          (write-gitignore! gitignore)))))))
