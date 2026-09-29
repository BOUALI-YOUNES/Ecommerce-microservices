package com.younes.notification.notification;

import org.springframework.data.mongodb.repository.MongoRepository;


public interface NotificationRepo extends MongoRepository<Notification , String>{

}
