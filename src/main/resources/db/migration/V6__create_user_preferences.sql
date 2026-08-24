CREATE TABLE user_preferences (
                                  id UUID NOT NULL,
                                  user_id UUID NOT NULL,
                                  currency VARCHAR(10) NOT NULL,
                                  language VARCHAR(10) NOT NULL,
                                  date_format VARCHAR(30) NOT NULL,
                                  units VARCHAR(20) NOT NULL,
                                  theme VARCHAR(20) NOT NULL,

                                  CONSTRAINT pk_user_preferences
                                      PRIMARY KEY (id),

                                  CONSTRAINT uk_user_preferences_user
                                      UNIQUE (user_id),

                                  CONSTRAINT fk_user_preferences_user
                                      FOREIGN KEY (user_id)
                                          REFERENCES users(id)
);