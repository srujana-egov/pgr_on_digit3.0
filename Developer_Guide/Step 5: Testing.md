# **Testing the PGR created using the following postman calls:**

## **1. Generating Auth Token:**
curl --location 'https://digit-lts.digit.org/keycloak/realms/PGRDEMO/protocol/openid-connect/token' \
--header 'Content-Type: application/x-www-form-urlencoded' \
--data-urlencode 'grant_type=password' \
--data-urlencode 'client_id=auth-server' \
--data-urlencode 'client_secret=changeme' \
--data-urlencode 'username=johndoe' \
--data-urlencode 'password=egov'

## **2. Create Service Request**
curl --location 'http://localhost:8083/citizen-service/create' \
--header 'Content-Type: application/json' \
--header 'X-Tenant-ID: PGRDEMO' \
--header 'Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJudTVsbkI5dmhZX2VNMDAtc2Z0NUdkVzdxbTh2RzBuTDItMnh2N3dvZnlvIn0.eyJleHAiOjE3NjI5NTU1NzcsImlhdCI6MTc2Mjk0ODM3NywianRpIjoiODMxNmYyN2QtODAxNy00MmU0LWI2YTEtZDlkM2Y2N2Q5MDNjIiwiaXNzIjoiaHR0cHM6Ly9kaWdpdC1sdHMuZGlnaXQub3JnL2tleWNsb2FrL3JlYWxtcy9QR1JERU1PIiwiYXVkIjoiYWNjb3VudCIsInN1YiI6ImZjN2ZlZDI1LWFkYTMtNDBjMy05MDM0LTFlNDA2ZmZhYTczMSIsInR5cCI6IkJlYXJlciIsImF6cCI6ImF1dGgtc2VydmVyIiwic2lkIjoiZjA3YjYyOWUtZmZjMi00MGQ0LTkwMWUtM2MwOTQwNmNlMTA5IiwiYWNyIjoiMSIsImFsbG93ZWQtb3JpZ2lucyI6WyIvKiJdLCJyZWFsbV9hY2Nlc3MiOnsicm9sZXMiOlsiZGVmYXVsdC1yb2xlcy1wZ3JkZW1vIiwiQ1NSIiwiR1JPIiwiU1VQRVJVU0VSIiwib2ZmbGluZV9hY2Nlc3MiLCJ1bWFfYXV0aG9yaXphdGlvbiJdfSwicmVzb3VyY2VfYWNjZXNzIjp7ImFjY291bnQiOnsicm9sZXMiOlsibWFuYWdlLWFjY291bnQiLCJtYW5hZ2UtYWNjb3VudC1saW5rcyIsInZpZXctcHJvZmlsZSJdfX0sInNjb3BlIjoicHJvZmlsZSBlbWFpbCIsImVtYWlsX3ZlcmlmaWVkIjp0cnVlLCJwcmVmZXJyZWRfdXNlcm5hbWUiOiJqb2huZG9lIiwiZW1haWwiOiJqb2huQGV4YW1wbGUuY29tIn0.NNCVkQpLdXWXxxLCQ0j1q2YSi6AYIXCk5Bggg9RMjEeNAxl2T0CEAvzRrh57t_SDdvGTUglJWqbUw07v4PuolapDZ9uozXWik7MRBOF8Koe3bZ4cFO5WOvvECKx4-00XGm7m5mAm9Ewg9aPrdJt1wGcafDWRQvTtVLTa4HNOTQWdLrjqgJU1H5WyIPH-uarYa_3sPI6BCG1NDnwPtwrTQGm41DvDCqX_Fr4SQ4o-UcBxiUaH0TmA3zFB8Nr6ygYYpJEWNGjB-zOk5afXwmSbLP1pcHVbRo9UTWChmP5roQMYknaUhOVsSNvtf54QTvS5H--RN3YTg15T85ANbLIo4A' \
--data-raw '{
  "CitizenService": {
    "serviceCode": "PGR001",
    "description": "Garbage not collected",
    "accountId": "CITIZEN-123",
    "fileStoreId": "e37fbd73-fee5-4990-8322-8fc6595d5d10",
    "boundaryCode": "DIST08",
    "applicationStatus": "ACTIVE",
    "email": "srujana.dadi@egovernments.org",
    "mobile": "9876543210"
  },
  "Workflow": {
    "action": "APPLY"
  }
}'

## **3. Update Service Request**
curl --location 'http://localhost:8083/citizen-service/update' \
--header 'Content-Type: application/json' \
--header 'X-Tenant-ID: PGRDEMO' \
--header 'X-Client-ID: admin' \
--header 'Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJudTVsbkI5dmhZX2VNMDAtc2Z0NUdkVzdxbTh2RzBuTDItMnh2N3dvZnlvIn0.eyJleHAiOjE3NjI5NTU1NzcsImlhdCI6MTc2Mjk0ODM3NywianRpIjoiODMxNmYyN2QtODAxNy00MmU0LWI2YTEtZDlkM2Y2N2Q5MDNjIiwiaXNzIjoiaHR0cHM6Ly9kaWdpdC1sdHMuZGlnaXQub3JnL2tleWNsb2FrL3JlYWxtcy9QR1JERU1PIiwiYXVkIjoiYWNjb3VudCIsInN1YiI6ImZjN2ZlZDI1LWFkYTMtNDBjMy05MDM0LTFlNDA2ZmZhYTczMSIsInR5cCI6IkJlYXJlciIsImF6cCI6ImF1dGgtc2VydmVyIiwic2lkIjoiZjA3YjYyOWUtZmZjMi00MGQ0LTkwMWUtM2MwOTQwNmNlMTA5IiwiYWNyIjoiMSIsImFsbG93ZWQtb3JpZ2lucyI6WyIvKiJdLCJyZWFsbV9hY2Nlc3MiOnsicm9sZXMiOlsiZGVmYXVsdC1yb2xlcy1wZ3JkZW1vIiwiQ1NSIiwiR1JPIiwiU1VQRVJVU0VSIiwib2ZmbGluZV9hY2Nlc3MiLCJ1bWFfYXV0aG9yaXphdGlvbiJdfSwicmVzb3VyY2VfYWNjZXNzIjp7ImFjY291bnQiOnsicm9sZXMiOlsibWFuYWdlLWFjY291bnQiLCJtYW5hZ2UtYWNjb3VudC1saW5rcyIsInZpZXctcHJvZmlsZSJdfX0sInNjb3BlIjoicHJvZmlsZSBlbWFpbCIsImVtYWlsX3ZlcmlmaWVkIjp0cnVlLCJwcmVmZXJyZWRfdXNlcm5hbWUiOiJqb2huZG9lIiwiZW1haWwiOiJqb2huQGV4YW1wbGUuY29tIn0.NNCVkQpLdXWXxxLCQ0j1q2YSi6AYIXCk5Bggg9RMjEeNAxl2T0CEAvzRrh57t_SDdvGTUglJWqbUw07v4PuolapDZ9uozXWik7MRBOF8Koe3bZ4cFO5WOvvECKx4-00XGm7m5mAm9Ewg9aPrdJt1wGcafDWRQvTtVLTa4HNOTQWdLrjqgJU1H5WyIPH-uarYa_3sPI6BCG1NDnwPtwrTQGm41DvDCqX_Fr4SQ4o-UcBxiUaH0TmA3zFB8Nr6ygYYpJEWNGjB-zOk5afXwmSbLP1pcHVbRo9UTWChmP5roQMYknaUhOVsSNvtf54QTvS5H--RN3YTg15T85ANbLIo4A' \
--data-raw ' {
            "CitizenService": {
                "serviceRequestId": "pgr-20251112-0012-R5",
                "serviceCode": "PGR001",
                "description": "Garbage not collected",
                "accountId": "CITIZEN-123",
                "source": "Citizen",
                "applicationStatus": "ACTIVE",
                "fileStoreId": "a6025bd5-2507-4942-87a8-1a16afc8833a",
                "fileValid": false,
                "boundaryCode": "DIST08",
                "boundaryValid": false,
                "action": "ASSIGN",
                "email": "srujana.dadi@egovernments.org",
                "mobile": "9876543210"
            },
            "Workflow": {
                "action": "ASSIGN",
                "assignes": null,
                "comments": null,
                "verificationDocuments": null
            }
        }'

## **4. Search Service Request**
curl --location 'http://localhost:8083/citizen-service/search?serviceRequestId=pgr-20251112-0001-GM' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJudTVsbkI5dmhZX2VNMDAtc2Z0NUdkVzdxbTh2RzBuTDItMnh2N3dvZnlvIn0.eyJleHAiOjE3NjI5NTA4MDQsImlhdCI6MTc2Mjk0MzYwNCwianRpIjoiMjU0MmRjOTAtODExNy00ODFlLTk1MGEtMGMxODBkNGZkMDhmIiwiaXNzIjoiaHR0cHM6Ly9kaWdpdC1sdHMuZGlnaXQub3JnL2tleWNsb2FrL3JlYWxtcy9QR1JERU1PIiwiYXVkIjoiYWNjb3VudCIsInN1YiI6ImZjN2ZlZDI1LWFkYTMtNDBjMy05MDM0LTFlNDA2ZmZhYTczMSIsInR5cCI6IkJlYXJlciIsImF6cCI6ImF1dGgtc2VydmVyIiwic2lkIjoiMzMwMmM0YzctOWEwYS00M2M1LTg4ZGItMTQ1NzFmNDM0NzRkIiwiYWNyIjoiMSIsImFsbG93ZWQtb3JpZ2lucyI6WyIvKiJdLCJyZWFsbV9hY2Nlc3MiOnsicm9sZXMiOlsiZGVmYXVsdC1yb2xlcy1wZ3JkZW1vIiwib2ZmbGluZV9hY2Nlc3MiLCJ1bWFfYXV0aG9yaXphdGlvbiJdfSwicmVzb3VyY2VfYWNjZXNzIjp7ImFjY291bnQiOnsicm9sZXMiOlsibWFuYWdlLWFjY291bnQiLCJtYW5hZ2UtYWNjb3VudC1saW5rcyIsInZpZXctcHJvZmlsZSJdfX0sInNjb3BlIjoicHJvZmlsZSBlbWFpbCIsImVtYWlsX3ZlcmlmaWVkIjp0cnVlLCJwcmVmZXJyZWRfdXNlcm5hbWUiOiJqb2huZG9lIiwiZW1haWwiOiJqb2huQGV4YW1wbGUuY29tIn0.aBl_lbg4yH4hCsCMZQpCiRsHgutxEhlrQ65OIsJrmGLuUGCH6kVCCqIt8EQNcUVkZihzQPB6HeG6-lJEKURhDkNmJmWxXWFvdQtGUQcgbdrkTaEleIghNOSNqu_a_MCsfNLus0hX85ptHQn3n09iMqwHqJEW417OAUljFbGQKjtc8QocBYiZdAmYfPtvBp_8gPwXpt4VwO82FfZQrBEqKq45QY8HnrC5I-yJEFj4xUDH6Q57a4a8ge5Jx6D4_uHCE0J85vOVwuvivQxpPZLi8mAIgUhANU4YvOTj575jlqHTIIq61gZOZBQop3OQF-sTzuu8zdXif1Vx8XcKvp68lg' \
--data ''

If all your calls are working, congratualtions your PGR is working!
