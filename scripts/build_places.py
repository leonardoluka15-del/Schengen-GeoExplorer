#!/usr/bin/env python3
"""Bundle GeoNames cities500 and administrative names for offline Android use.
GeoNames data: CC BY 4.0, https://www.geonames.org/ .
Coverage: populated places >=500 people plus some administrative seats; NOT all villages.
"""
import csv, io, os, urllib.request, zipfile
from collections import defaultdict
from pathlib import Path

CODES = dict(zip(
"Austria Belgium Bulgaria Croatia Czechia Denmark Estonia Finland France Germany Greece Hungary Iceland Italy Latvia Liechtenstein Lithuania Luxembourg Malta Netherlands Norway Poland Portugal Romania Slovakia Slovenia Spain Sweden Switzerland".split(),
"AT BE BG HR CZ DK EE FI FR DE GR HU IS IT LV LI LT LU MT NL NO PL PT RO SK SI ES SE CH".split()))
base="https://download.geonames.org/export/dump/"
def fetch(name):
    print("Downloading",name,flush=True)
    with urllib.request.urlopen(base+name,timeout=180) as response:
        return response.read()
def lines(name):
    return fetch(name).decode("utf-8-sig").splitlines()
a1={}
for line in lines("admin1CodesASCII.txt"):
    p=line.split("\t")
    if len(p)>=2:a1[p[0]]=p[1]
a2={}
for line in lines("admin2Codes.txt"):
    p=line.split("\t")
    if len(p)>=2:a2[p[0]]=p[1]
by_code={v:k for k,v in CODES.items()}
records=defaultdict(set)
data=fetch("cities500.zip")
with zipfile.ZipFile(io.BytesIO(data)) as z:
    with z.open("cities500.txt") as f:
        for raw in f:
            p=raw.decode("utf-8").rstrip("\n").split("\t")
            if len(p)<15 or p[8] not in by_code:continue
            code=p[8]; reg=p[10]; sub=p[11]
            region=a1.get(code+"."+reg,reg or "Unspecified region")
            province=a2.get(code+"."+reg+"."+sub,sub or "Unspecified subdivision")
            name=p[1].replace("|"," ").replace("\t"," ")
            if name:records[code].add((region,province,name))
out=Path("app/src/main/assets/places.tsv")
out.parent.mkdir(parents=True,exist_ok=True)
with out.open("w",encoding="utf-8") as f:
    for code in sorted(records):
        for region,province,city in sorted(records[code]):
            f.write("\t".join((code,region,province,city))+"\n")
print("Offline place records:",sum(map(len,records.values())),flush=True)
