import requests

cookies = {
    'AMCV_4D6368F454EC41940A4C98A6%40AdobeOrg': '179643557%7CMCIDTS%7C20146%7CMCMID%7C35620687361685143708671614397990958274%7CMCAID%7CNONE%7CMCOPTOUT-1740540362s%7CNONE%7CvVersion%7C5.5.0',
    '__zlcmid': '1QxnVZPFFXDFoMj',
    'acceptcookies': 'false',
    'fcsid': 'nosfgnrc57f8ur1ju0m0vvv5ap',
    'AMP_572175c4a8': 'JTdCJTIyZGV2aWNlSWQlMjIlM0ElMjJhN2Y4ZDQwNS04NzE5LTRjNTQtODM4MC1iOTQ3ZDRhYTE3ZWMlMjIlMkMlMjJ1c2VySWQlMjIlM0ElMjI3ODUxOTk5YTYwN2ZjOWI2Y2EyMDUwNjM1NWVkMTgxN2U4NjcwMDcwNzI4NTIxZjNlY2I1YzIzNTcwNjAzMzA0JTIyJTJDJTIyc2Vzc2lvbklkJTIyJTNBMTc0NDgxNTIxNjQzMiUyQyUyMm9wdE91dCUyMiUzQWZhbHNlJTJDJTIybGFzdEV2ZW50VGltZSUyMiUzQTE3NDQ4MTU1MDUyNjQlMkMlMjJsYXN0RXZlbnRJZCUyMiUzQTQ1JTJDJTIycGFnZUNvdW50ZXIlMjIlM0E3JTdE',
    'AMP_MKTG_572175c4a8': 'JTdCJTIycmVmZXJyZXIlMjIlM0ElMjJodHRwcyUzQSUyRiUyRnd3dy5nb29nbGUuY29tJTJGJTIyJTJDJTIycmVmZXJyaW5nX2RvbWFpbiUyMiUzQSUyMnd3dy5nb29nbGUuY29tJTIyJTdE',
    'amplitude_id_9f6c0bb8b82021496164c672a7dc98d6_edmbrown.edu': 'eyJkZXZpY2VJZCI6IjdiNGEzZmQ2LTdjMmUtNGZjZC1iMzUxLTRjNTg1OTI1ODE5M1IiLCJ1c2VySWQiOm51bGwsIm9wdE91dCI6ZmFsc2UsInNlc3Npb25JZCI6MTc0NDY2MzMxODU5MywibGFzdEV2ZW50VGltZSI6MTc0NDY2MzMyMzg5OCwiZXZlbnRJZCI6MCwiaWRlbnRpZnlJZCI6MTUsInNlcXVlbmNlTnVtYmVyIjoxNX0=',
    'IDMSESSID': '5F4F6FE688A6974A45BB5DDF724B25B1D43F9B1F2904B921CD5E4D2BB297C5AC19E573A1A6BE903A2A963193973A2681',
    'TS01b3a32b': '014b44e76b2bfffa4bf11f672f482dda08cb75db42a613534f113167d5bf454488e2a1502795632863ba48809e01be925dd9ca685b',
}

headers = {
    'User-Agent': 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:137.0) Gecko/20100101 Firefox/137.0',
    'Accept': 'application/json, text/javascript, */*; q=0.01',
    'Accept-Language': 'en-US,en;q=0.5',
    # 'Accept-Encoding': 'gzip, deflate, br, zstd',
    'Content-Type': 'application/json',
    'X-Requested-With': 'XMLHttpRequest',
    'Origin': 'https://cab.brown.edu',
    'Connection': 'keep-alive',
    'Referer': 'https://cab.brown.edu/',
    # 'Cookie': 'AMCV_4D6368F454EC41940A4C98A6%40AdobeOrg=179643557%7CMCIDTS%7C20146%7CMCMID%7C35620687361685143708671614397990958274%7CMCAID%7CNONE%7CMCOPTOUT-1740540362s%7CNONE%7CvVersion%7C5.5.0; __zlcmid=1QxnVZPFFXDFoMj; acceptcookies=false; fcsid=nosfgnrc57f8ur1ju0m0vvv5ap; AMP_572175c4a8=JTdCJTIyZGV2aWNlSWQlMjIlM0ElMjJhN2Y4ZDQwNS04NzE5LTRjNTQtODM4MC1iOTQ3ZDRhYTE3ZWMlMjIlMkMlMjJ1c2VySWQlMjIlM0ElMjI3ODUxOTk5YTYwN2ZjOWI2Y2EyMDUwNjM1NWVkMTgxN2U4NjcwMDcwNzI4NTIxZjNlY2I1YzIzNTcwNjAzMzA0JTIyJTJDJTIyc2Vzc2lvbklkJTIyJTNBMTc0NDgxNTIxNjQzMiUyQyUyMm9wdE91dCUyMiUzQWZhbHNlJTJDJTIybGFzdEV2ZW50VGltZSUyMiUzQTE3NDQ4MTU1MDUyNjQlMkMlMjJsYXN0RXZlbnRJZCUyMiUzQTQ1JTJDJTIycGFnZUNvdW50ZXIlMjIlM0E3JTdE; AMP_MKTG_572175c4a8=JTdCJTIycmVmZXJyZXIlMjIlM0ElMjJodHRwcyUzQSUyRiUyRnd3dy5nb29nbGUuY29tJTJGJTIyJTJDJTIycmVmZXJyaW5nX2RvbWFpbiUyMiUzQSUyMnd3dy5nb29nbGUuY29tJTIyJTdE; amplitude_id_9f6c0bb8b82021496164c672a7dc98d6_edmbrown.edu=eyJkZXZpY2VJZCI6IjdiNGEzZmQ2LTdjMmUtNGZjZC1iMzUxLTRjNTg1OTI1ODE5M1IiLCJ1c2VySWQiOm51bGwsIm9wdE91dCI6ZmFsc2UsInNlc3Npb25JZCI6MTc0NDY2MzMxODU5MywibGFzdEV2ZW50VGltZSI6MTc0NDY2MzMyMzg5OCwiZXZlbnRJZCI6MCwiaWRlbnRpZnlJZCI6MTUsInNlcXVlbmNlTnVtYmVyIjoxNX0=; IDMSESSID=5F4F6FE688A6974A45BB5DDF724B25B1D43F9B1F2904B921CD5E4D2BB297C5AC19E573A1A6BE903A2A963193973A2681; TS01b3a32b=014b44e76b2bfffa4bf11f672f482dda08cb75db42a613534f113167d5bf454488e2a1502795632863ba48809e01be925dd9ca685b',
    'Sec-Fetch-Dest': 'empty',
    'Sec-Fetch-Mode': 'cors',
    'Sec-Fetch-Site': 'same-origin',
    'DNT': '1',
    'Sec-GPC': '1',
    'Priority': 'u=0',
    # Requests doesn't support trailers
    # 'TE': 'trailers',
}

def get_course_requirements(course_code, term="202510"):
    search_url = 'https://cab.brown.edu/api/'
    details_url = 'https://cab.brown.edu/api/?page=fose&route=details'

    # Reuse your cookies and headers as-is
    # cookies = { ... }  # Keep yours unchanged
    # headers = { ... }  # Keep yours unchanged

    # Step 1: Search for the course code
    search_payload = {
        "other": {
            "srcdb": term
        },
        "criteria": [
            {"field": "code", "value": course_code},
            {"field": "is_ind_study", "value": "N"},
            {"field": "is_canc", "value": "N"}
        ]
    }

    search_params = {
        'page': 'fose',
        'route': 'search',
        
    }

    search_response = requests.post(
        search_url,
        params=search_params,
        cookies=cookies,
        headers=headers,
        json=search_payload
    )
    search_data = search_response.json()
    results = search_data.get("results", [])

    if not results:
        print(f"No results found for {course_code}")
        return
    
    # Step 2: Use CRN and srcdb to get detailed course info
    crn = results[0]['crn']
    srcdb = results[0]['srcdb']

    detail_payload = {
        "srcdb": srcdb,
        "crn": crn,
        "group": f"code:{course_code}"
    }


    detail_response = requests.post(
        details_url,
        cookies=cookies,
        headers=headers,
        json=detail_payload
    )

    detail_data = detail_response.json()


    # Step 3: Extract requirements
    prereq_html = detail_data.get("registration_restrictions", "")
    permission_required = detail_data.get("permreq", "N") == "Y"
    description = detail_data.get("description", "")

    print(f"\n {course_code}: Requirements")
    if prereq_html:
        print("From registration restrictions:")
        print(prereq_html)
    else:
        print("No formal registration restrictions.")

    if permission_required:
        print("Instructor or department permission is required.")

    if "prerequisite" in description.lower() or "recommended" in description.lower():
        print("\nFrom course description (may be soft prereqs):")
        print(description)

# Example usage:
get_course_requirements("CHEM 0360")



# params = {
#     'page': 'fose',
#     'route': 'search',
# }

# json_data = {
#     "other": {
#         "srcdb": "202510"  # e.g. Fall 2025
#     },
#     "criteria": [
#         # {"field": "keyword", "value": "engl"},           # was incorrectly "hist" in your encoded data
#         {"field": "is_ind_study", "value": "N"},
#         {"field": "is_canc", "value": "N"}
#     ]
# }

# response = requests.post(
#     'https://cab.brown.edu/api/',
#     params=params,
#     cookies=cookies,
#     headers=headers,
#     json=json_data  # ✅ Proper JSON body
# )

# data = response.json()

# print(data)
# for item in data.get("results", []):
#     print(f"{item['code']}: {item['title']}")