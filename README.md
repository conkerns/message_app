# Readme

## API

The application exposes 6 endpoints. When the application is running they can be explored via the swagger page available at `http://localhost:9000/posting-service/swagger-ui.html`. All the endpoints should be prefixed with `http://localhost:9000/posting-service`.

### 1. POST /users/{username}/post

Used for posting new messages. 

Required path variable `username` is the user that is posting the message.
The body of the request must be a JSON object with a single field called _post_ e.g.:
~~~~
{
  "post": "tweeeeeet"
}
~~~~
The service creates the user if not already present and validates that the message is present and not over 140 characters long.

### 2. PUT /users/{username}/follow?followedUserName=...

Used to make one user follow another.

Required path variable `username` is the user that is requesting to follow another.

The `followedUserName` parameter represents the name of the user that is supposed to be followed.

Both of the users need to already exist, otherwise a validation error is thrown. Attempting to follow yourself causes a validation error as well.

### 3. GET /users/{username}/completeWall

Returns **all** the posts submitted by the user provided in `username` in reverse chronological order. If the user doesn't exist, a validation error is thrown. The posts are returned as a JSON array, e.g.:
~~~~
[
  {
    "username": "adrian",
    "content": "tweeeeeet 2",
    "createdDate": "2018-10-21T15:38:36.878"
  },
  {
    "username": "adrian",
    "content": "tweeeeeet 1",
    "createdDate": "2018-10-21T12:33:32.012"
  }
]
~~~~ 

### 4. GET /users/{username}/completeTimeline

Returns **all** the posts submitted by the users followed by the user provided in `username` in reverse chronological order. If the user doesn't exist, a validation error is thrown. Format is the same as in **3**.

### 5. GET /users/{username}/wall?page=...&size=...

A paged version of the `completeWall` endpoint for returning blocks of the user's posts, also in reverse chronological order. Both parameters must be present. `page` defines the number of the currently requested page (starting from 0). `size` is the number of posts a page should have. The body of the response is the same as in **3**. If the provided `page` parameter is larger than the number of available pages a validation error is thrown.

> It would be a good idea to also add an endpoint that returned information about the available number of pages and/or put that information into this endpoint's response.

> The results are not cached or otherwise saved in memory, so in the current version it's possible to get duplicate posts in subsequent calls with incrememted page number due to the possibility that a new post was added in the meantime.

### 6. GET /users/{username}/timeline?page=...&size=...

Same as **5**, but for the `completeTimeline` endpoint.

> Using any of the paged endpoints withou providing both the `page` and `size` parameters will result in a 404 status code response.

## Running locally

After cloning the repository and importing to an IDE (tested in Intellij) there are three ways to run the application:
* simply run the main method `posting.Application.main`
* execute the `run` goal on the `spring-boot` plugin found in the `app` module
* execute the `package` goal on the root module (`posting-service`). This will create a runnable jar in the `target` directory of the `app` module which can be run using the basic `java -jar` command 