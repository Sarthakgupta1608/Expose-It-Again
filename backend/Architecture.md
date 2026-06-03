
# Architecture:


## Registration Form:
* Frontend: Username, Password, Email, Avatar(default)
* Backend: ID, Username(Unique), Email(Unique) Created_At, Role

## Login/Signup:
* JWT
* JWT Stored in Cookies
* Refresh Token stored in database


## Post Creation Form:
* Frontend: Title, Description, Location, Media Files, Tags(Education, Garbage, ...)
* Backend: ID, Created_At, User(Reference), Status(Active), Verified(Boolean), Likes, Dislikes

## Comments:
* Frontend: Content
* Backend: ID, User(Reference), Parent_Comment(ID), Created_At, Likes


## Home:
### Frontend:
* Trending Post:
  * Fetched from Redis
  * Redis Store based on ZSET
* Filter: Tags, Location, New, Trending
* Post

## Post:
## Profile:
