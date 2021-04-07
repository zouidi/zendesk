# zendesk
There is no way to get all zendesk visitors ids and phone numbers. I did this in other way:

1- Loading all chats history from api

2- Extracting zendesk visitors ids and phones number from loaded chats and save it in data.txt file

3-  Removing duplicated values from data.txt file

4- Finding user id by phone number  in your database and replacing the zendesk visitor phone number with this user id
