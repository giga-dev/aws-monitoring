1. Have your aws credential in the ~/.aws/credentials
2. Write your email.properties file in the cwd something like

````Bash
   fromUser=myemail@myorg
   fromUserEmailPassword=mysecretpaswword
````

3. build the uber jar using `mvn install`
4. run the uber jar 
`java -cp /home/barak/.m2/repository/com/gigasapces/aws/1.0-SNAPSHOT/aws-1.0-SNAPSHOT.jar com.gigaspaces.Loop`

