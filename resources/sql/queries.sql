-- :name insert-day! :! :n
-- :doc adds the predictions for this day
INSERT INTO predictions
(day, m01, m02, m03, m04)
VALUES (:day, :m01, :m02, :m03, :m04)
on conflict (day) do update
    set m01 = :m01, m02 = :m02, m03 = :m03, m04 = :m04;

-- :name update-day! :! :n
-- :doc updates predictions for this day
UPDATE predictions
set m01 = :m01, m02 = :m02, m03 = :m03, m04 = :m04
where day = :day;

-- :name get-by-saved-on :? :1
-- :doc retrieves the predictions by the saved on date
SELECT * FROM predictions
WHERE  day = :day;

-- :name get-latest :? :1
-- :doc Latest recorded row by date
SELECT * FROM predictions
ORDER BY day DESC LIMIT 1;

-- :name get-all-predictions :? :*
-- :doc Returns all values from the predictions table
SELECT * FROM predictions
ORDER BY day DESC;
