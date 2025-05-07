# **About**

This is a simple bank application based on the requirements of
MarlowNavigation assignment, which required "Allow users to deposit and 
withdraw money, and allow simultaneous access of an account"

## Requirements

1. Java/OpenJDK Installed
2. SBT Installed
3. Postgresql Installed

IDE- to check out the code(Intellij preferred)

## Steps

Pull the code to your local system

Run the sql file present in conf/evolution/default/1.sql to setup database 

Steps to run the sql file
1. open terminal
2. "psql postgres" OR "psql -u postgres" command
3. run the 1.sql script by using command - "\i [PATH_TO_DIR]/marlownavproject/conf/evolutions/default/1.sql"


To Run application- 
1. cd into the marlownavproject folder
2. sbt run

The application starts at localhost:9000

Check the conf/routes file for all the REST API endpoints
available with examples(explained below as well)-


### Tha application has two Controllers -

**UserController** - for handling all user related requests,
        for now just creating a user

Rest Endpoints - 
1. GET /user/create <br>
        Request-{name:String, address:String} <br>
        Response - 200 Ok


**AccountController** - handling all account related requests such as creating an account,
crediting, debiting, transferring from one account to another account

Rest Endpoints -
1. GET /account/create <br>
   Request-
````
{
    type:String, 
    balance:Int, 
    owners: Array[Int], #user ids of the account owners  
    withdrawalLimit:Int
}
````
   
      #Example  { 
            type:"Saving", 
            balance:1000, 
            owners: [1,2], 
            withdrawalLimit:10
        }
   
   Response - 200 Ok
2. GET /account/credit <br>
   Request-
````
{
    accountId:Int, 
    userId:Int, 
    amount: Int,
    memo:String
}
````

      #Example  { 
            accountId:2, 
            userId:3, 
            amount: 50,
            memo: "go have fun"
        }

Response - 200 Ok

3. GET /account/debit <br>
   Request-
````
{
    accountId:Int, 
    userId:Int, 
    amount: Int,
    memo:String
}
````

      #Example  { 
            accountId:2, 
            userId:3, 
            amount: 50,
            memo: "gas bill"
        }

Response - 200 Ok

4. GET /account/transfer <br>
   Request-
```
{
    senderAccountId:Int, 
    receiverAccountId:Int,
    senderId:Int, 
    amount: Int,
    memo:String
}
```

      #Example  { 
        senderAccountId:Int, 
        receiverAccountId:Int,
        senderId:Int, 
        amount: Int,
        memo:String
}

Response - 200 Ok



Kafka - A POC of Kafka module has been added to show these APIs can work
using the queue messaging service of Kafka. Check the services folder for more.






