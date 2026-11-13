# Lists every Maya Sawa route with its auth dependencies and auth-looking names in the handler body.
# Run from the maya-sawa repo:  poetry run python <this file>   (imports the app; no server needed)
# Static heuristics only: "deps= inline=" means no auth was found, not that none exists. Read the handler.
import inspect, re, json
from fastapi.routing import APIRoute
from maya_sawa.main import app
KEY = re.compile(r"require_\w+|agent_key|X-Market-Agent-Key|_require\w*|verify_\w+|authorize\w*|bearer|owner\w*|internal_secret|manage_users|publisher_user|analysis_editor|_member", re.I)
def deps(d, out):
    for s in d.dependencies:
        if s.call: out.add(getattr(s.call, "__name__", str(s.call)))
        deps(s, out)
rows=[]
for r in app.routes:
    if not isinstance(r, APIRoute): continue
    names=set(); deps(r.dependant, names)
    try: src=inspect.getsource(r.endpoint)
    except Exception: src=""
    inline=sorted(set(m.group(0) for m in KEY.finditer(src)))
    for m in sorted(r.methods - {"HEAD","OPTIONS"}):
        rows.append((r.path, m, sorted(n for n in names if n not in ("get_blog",)), inline))
for p,m,d,i in sorted(rows): print(f"{m:6} {p} | deps={','.join(d)} | inline={','.join(i)}")
