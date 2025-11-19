# Configuration using CLI

Now that our PGR code is up and running we need to configure the DIGIT services. Luckily that is very easy and can be done in a few steps.

For more information refer here: https://github.com/digitnxt/digit3/tree/develop/code/Tools/DIGIT-CLI

## **Steps** 

1. Clone the source code: https://github.com/digitnxt/digit3/tree/2b4abbaf839ded28a9d39bed4335a3147b138426/code/Tools/DIGIT-CLI
2. Navigate to digit3 > code > tools > DIGIT-CLI
3. Run the command: GOOS=darwin GOARCH=amd64 go build -o digit .
4. Create an account first:
./digit create-account --name travisscot --email sahoo123@gmail.com --server https://digit-lts.digit.org
5. Configure the CLI with the obtained credentials for future use:
./digit config set --server https://digit-lts.digit.org --realm TRAVISSCOT  --client-id auth-server --client-secret changeme --username sahoo123@gmail.com --password default
6. Create additional users (after configuration):
./digit  create-user --username johndoe --password mypassword --email john@example.com
7. Reset a user's password:
./digit reset-password --username johndoe --new-password egov
8. Search for user:
./digit search-user --username johndoe
9. Create an ID generation template:
./digit create-idgen-template --template-id ossrgIssd --template "{ORG}-{DATE:yyyyMMdd}-{SEQ}-{RAND}" --scope daily --start 1 --padding-length 4 --padding-char "0" --random-length 2 --random-charset "A-Z0-9"
10. Create a complete workflow from YAML:
./digit create-workflow --file example-workflow.yaml
11. Create a notification template:
./digit create-template --template-id "my-template" --version "1.0.0" --type "EMAIL" --subject "Test Subject" --content "Test Content"
12. Create role:
./digit create-role --role-name GRO --description "Administrator role"
13. Assign role to user:
./digit assign-role --username johndoe --role-name SUPERUSER
