-- RG-22 : le verrouillage se declenche sur 5 echecs *dans une fenetre de 15 minutes*, pas sur
-- 5 echecs cumules depuis toujours. Il faut savoir quand a eu lieu le dernier echec pour
-- decider si le compteur doit repartir de zero.

ALTER TABLE users
    ADD COLUMN last_failed_attempt_at TIMESTAMPTZ;
