(ns apollo.core-test
  (:require [apollo.core :refer :all]
            [apollo.db.schema :as s]
            [clojure.test :refer :all]))

(defn request [handler method resource & params]
  (handler {:uri resource :request-method method :params params}))

(deftest is-dev-true
  (testing "is dev true"
    (is (= is-dev? true))))

(deftest is-inject-devcode-working
  (testing "given is-dev? is true we should see some injecting of figwheel code"
    (is (boolean (re-find #"('apollo.client.dev')" (:body (render-to-response (page))))))))

(deftest redirects-to-index
  (testing "redirects from / to index.html"
    (let [{status :status {location "Location"} :headers} (request app-handler :get "/")]
      (is (= 302 status))
      (is (= location "/index.html")))))

(deftest get-index
  (testing "getting index.html"
    (let [{status :status} (request app-handler :get "/index.html")]
      (is (= 200 status)))))

(deftest recently-added
  (testing "get list of recently added"
    (let [{status :status} (request app-handler :get "/api/recently-added")]
      (is (= 200 status)))))

(deftest by-year
  (testing "list by year"
    (let [{status :status} (request app-handler :get "/api/by-year")]
         (is (= 200 status)))))


(deftest get-mounts
  (testing "get list of mounts"
    (let [{status :status} (request app-handler :get "/api/mounts")]
      (is (= 200 status)))))


(deftest artist-api
  (testing "hit artist api"
    (let [{artist :artist_id album :id} (first (s/get-albums-recently-added))
          {id :id} (first (s/tracks-by-album album))]
      (is (= 200 (:status (request app-handler :get (str "/api/artists/" artist "/info")))))
      (is (= 404 (:status (request app-handler :get (str "/api/artists/" artist "/image")))))
      (is (= 200 (:status (request app-handler :get (str "/api/artists/" artist )))))
      (is (= 200 (:status (request app-handler :get (str "/api/artists/" artist "/albums/" album)))))
      (is (= 200 (:status (request app-handler :get (str "/api/artists/" artist "/albums/" album "/image")))))
      (is (= 200 (:status (request app-handler :get (str "/api/artists/" artist "/albums/" album "/zip")))))
      (is (= 200 (:status (request app-handler :get (str "/api/artists/" artist "/albums/" album "/tracks/" id)))))
      (is (= 200 (:status (request app-handler :get (str "/api/artists/search/" (str (take 3 artist))))))))))
