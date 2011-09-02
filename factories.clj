(use 'clj-factory.core)
(use 'jiksnu.abdera)
(import 'jiksnu.model.Activity)
(import 'jiksnu.model.Domain)
(import 'jiksnu.model.Subscription)
(import 'jiksnu.model.User)

(defseq :id
  [n]
  n)

(defseq :domain
  [n]
  (str "example" n ".com"))

(defseq :word
  [n]
  (str "word" n))

(deffactory Activity
  {:id #'new-id
   :title (fseq :word)
   :summary (fseq :word)
   :published #'sugar/date
   :updated #'sugar/date
   :public true})

(deffactory Domain
  {:_id (fseq :domain)
   :local false})

(deffactory Subscription
  {:to (fseq :word)
   :from (fseq :word)
   :created #'sugar/date})

(deffactory User
  (let [password (fseq :word)]
    {:username (fseq :word)
     :domain (config :domain)
     :name (fseq :word)
     :first-name (fseq :word)
     :last-name (fseq :word)
     :password password
     :confirm-password password}))
