// Academy ERP — dynamic calculations, localStorage
const LS_KEY = 'academy_erp_v1';
let db = load() || { courses: [], students: [], attendance: {}, payments: {} };
// attendance: {"studentId|YYYY-MM-DD": "present"|"excused"|"counted"}
// payments: {"studentId|cycleStartISO": true}

function load(){ try{ return JSON.parse(localStorage.getItem(LS_KEY)); }catch{ return null; } }
function save(){ localStorage.setItem(LS_KEY, JSON.stringify(db)); }
const $ = id => document.getElementById(id);
const uid = () => Math.random().toString(36).slice(2,9);
const iso = d => new Date(d).toISOString().slice(0,10);
const addDays = (d,n)=>{ const x=new Date(d); x.setDate(x.getDate()+n); return x; };
const fmt = s => s ? new Date(s+'T00:00:00').toLocaleDateString() : '—';

function calcAge(dobStr){
  if(!dobStr) return '';
  const b=new Date(dobStr), t=new Date();
  let a=t.getFullYear()-b.getFullYear();
  const m=t.getMonth()-b.getMonth();
  if(m<0||(m===0&&t.getDate()<b.getDate())) a--;
  return a;
}

// ---- Sessions: 1 per week from course.startDate ----
function courseSessions(course, count=24){
  const out=[]; let d=new Date(course.startDate+'T00:00:00');
  for(let i=0;i<count;i++){ out.push(iso(d)); d=addDays(d,7); }
  return out;
}
// monthly hours
function monthlyHours(course, count=24){
  const map={};
  courseSessions(course,count).forEach(dt=>{
    const k=dt.slice(0,7);
    map[k]=(map[k]||0)+1;
  });
  return Object.entries(map).sort().map(([month,sessions])=>({
    month, sessions, totalHours: sessions*Number(course.hoursPerSession)
  }));
}

// ---- Subscription: 4 counted sessions ----
function excusedCount(studentId, courseId){
  return Object.entries(db.attendance).filter(([k,v])=>{
    const [sid,date]=k.split('|');
    if(sid!==studentId||v!=='excused') return false;
    const st=db.students.find(s=>s.id===sid);
    return st && st.courseId===courseId;
  }).length;
}
function studentCycle(student, course){
  // first 5 weekly sessions from subStart (5th needed only if excused used)
  let d=new Date(student.subStart+'T00:00:00');
  const sess=[0,1,2,3,4].map(i=>iso(addDays(d,i*7)));
  const exc = excusedCount(student.id, course.id);
  const usesExcuse = exc>=1; // 1 excused per whole course extends this cycle if it falls inside
  // check if the excused date is inside this 5-session window, else don't extend
  let excInWindow=false;
  if(usesExcuse){
    excInWindow=Object.entries(db.attendance).some(([k,v])=>{
      const [sid,date]=k.split('|');
      return sid===student.id&&v==='excused'&&sess.includes(date);
    });
  }
  const endDate = excInWindow ? sess[4] : sess[3];
  const countedSessions = excInWindow ? sess : sess.slice(0,4);
  return { sessions: countedSessions, allFive: sess, start: sess[0], end: endDate, extended: excInWindow };
}
function daysLeft(endIso){
  const t=new Date(); t.setHours(0,0,0,0);
  return Math.ceil((new Date(endIso+'T00:00:00')-t)/86400000);
}
function isPaid(studentId, cycleStart){ return !!db.payments[studentId+'|'+cycleStart]; }

// ---------- Tabs ----------
document.querySelectorAll('nav button').forEach(b=>b.onclick=()=>{
  document.querySelectorAll('nav button').forEach(x=>x.classList.remove('active'));
  document.querySelectorAll('.tab').forEach(x=>x.classList.remove('active'));
  b.classList.add('active'); $('tab-'+b.dataset.tab).classList.add('active');
});

// ---------- Courses ----------
$('courseForm').onsubmit=e=>{
  e.preventDefault();
  const id=$('courseId').value||uid();
  const rec={ id, name:$('courseName').value.trim(),
    hoursPerSession:Number($('courseHours').value),
    startDate:$('courseStart').value, weekday:Number($('courseWeekday').value),
    price:Number($('coursePrice').value||0) };
  const i=db.courses.findIndex(c=>c.id===id);
  if(i>=0) db.courses[i]=rec; else db.courses.push(rec);
  save(); e.target.reset(); $('courseId').value=''; renderAll();
};
$('courseReset').onclick=()=>{ $('courseForm').reset(); $('courseId').value=''; };

function renderCourses(){
  refreshCourseSelects();
  $('courseList').innerHTML = db.courses.map(c=>{
    const mh=monthlyHours(c,12);
    const rows=mh.map(m=>`<tr><td>${m.month}</td><td>${m.sessions}</td><td>${m.totalHours}</td></tr>`).join('');
    const sess=courseSessions(c,8).map(fmt).join(', ');
    const n=db.students.filter(s=>s.courseId===c.id).length;
    return `<div class="card"><b>${c.name}</b> — ${c.hoursPerSession}h/session — starts ${fmt(c.startDate)} — ${n} students — price ${c.price||'—'}/4 sessions
      <br><small>Next sessions: ${sess}</small>
      <table><tr><th>Month</th><th>Sessions</th><th>Total hours</th></tr>${rows}</table>
      <button onclick="editCourse('${c.id}')">Edit</button>
      <button onclick="delCourse('${c.id}')" class="danger">Delete</button></div>`;
  }).join('') || '<p>No courses yet.</p>';
}
window.editCourse=id=>{ const c=db.courses.find(x=>x.id===id);
  $('courseId').value=c.id; $('courseName').value=c.name; $('courseHours').value=c.hoursPerSession;
  $('courseStart').value=c.startDate; $('courseWeekday').value=c.weekday; $('coursePrice').value=c.price||''; };
window.delCourse=id=>{ if(!confirm('Delete course + its students?'))return;
  db.courses=db.courses.filter(c=>c.id!==id);
  db.students=db.students.filter(s=>s.courseId!==id); save(); renderAll(); };

// ---------- Students ----------
$('studentDob').onchange=()=>{ $('studentAge').value=calcAge($('studentDob').value); };
$('studentForm').onsubmit=e=>{
  e.preventDefault();
  if(!db.courses.length) return alert('Create a course first.');
  const id=$('studentId').value||uid();
  const rec={ id, name:$('studentName').value.trim(), dob:$('studentDob').value,
    phone:$('studentPhone').value.trim(), courseId:$('studentCourse').value,
    subStart:$('studentSubStart').value };
  const i=db.students.findIndex(s=>s.id===id);
  if(i>=0) db.students[i]=rec; else db.students.push(rec);
  save(); e.target.reset(); $('studentId').value=''; $('studentAge').value=''; renderAll();
};
$('studentReset').onclick=()=>{ $('studentForm').reset(); $('studentId').value=''; };
$('filterCourse').onchange=renderStudents; $('searchStudent').oninput=renderStudents;

function refreshCourseSelects(){
  const opts=db.courses.map(c=>`<option value="${c.id}">${c.name} (${c.hoursPerSession}h)</option>`).join('');
  $('studentCourse').innerHTML=opts; $('attCourse').innerHTML=opts;
  $('filterCourse').innerHTML='<option value="">All</option>'+opts;
}
function renderStudents(){
  const f=$('filterCourse').value, q=($('searchStudent').value||'').toLowerCase();
  const list=db.students.filter(s=>(!f||s.courseId===f)&&(!q||s.name.toLowerCase().includes(q)||(s.phone||'').includes(q)));
  $('studentList').innerHTML=`<table><tr><th>Name</th><th>Age</th><th>Phone</th><th>Course</th><th>Sub start</th><th>Sub end</th><th>Left</th><th>Payment</th><th></th></tr>${
    list.map(s=>{
      const c=db.courses.find(x=>x.id===s.courseId)||{};
      const cyc=c.id?studentCycle(s,c):{start:'—',end:'—',extended:false};
      const dl=c.id?daysLeft(cyc.end):'—';
      const paid=isPaid(s.id,cyc.start);
      return `<tr><td>${s.name}</td><td>${calcAge(s.dob)} (${fmt(s.dob)})</td><td>${s.phone||''}</td>
        <td>${c.name||'?'} (${c.hoursPerSession||'?'}h)</td>
        <td>${fmt(cyc.start)}</td><td>${fmt(cyc.end)}${cyc.extended?' (+1 excused)':''}</td>
        <td>${dl}${dl!=='—'&&dl<0?' (expired)':''}</td>
        <td><span class="badge ${paid?'paid':'unpaid'}">${paid?'PAID':'NOT PAID'}</span></td>
        <td><button onclick="editStudent('${s.id}')">Edit</button>
        <button onclick="renewSub('${s.id}')">Renew (+4)</button>
        <button onclick="delStudent('${s.id}')" class="danger">X</button></td></tr>`;
    }).join('')}</table>`;
}
window.editStudent=id=>{ const s=db.students.find(x=>x.id===id);
  $('studentId').value=s.id; $('studentName').value=s.name; $('studentDob').value=s.dob;
  $('studentAge').value=calcAge(s.dob); $('studentPhone').value=s.phone||'';
  $('studentCourse').value=s.courseId; $('studentSubStart').value=s.subStart; };
window.delStudent=id=>{ if(!confirm('Delete student?'))return;
  db.students=db.students.filter(s=>s.id!==id); save(); renderAll(); };
window.renewSub=id=>{ const s=db.students.find(x=>x.id===id); const c=db.courses.find(x=>x.id===s.courseId);
  const cyc=studentCycle(s,c); s.subStart=iso(addDays(new Date(cyc.end+'T00:00:00'),7)); save(); renderAll();
  alert(`Renewed. New cycle starts ${s.subStart}`); };

// ---------- Attendance & payments ----------
$('attCourse').onchange=renderAttDates; $('attDate').onchange=renderAtt;
function renderAttDates(){
  const c=db.courses.find(x=>x.id===$('attCourse').value); if(!c) return;
  $('attDate').innerHTML=courseSessions(c,12).map(d=>`<option value="${d}">${d} (${fmt(d)})</option>`).join('');
  renderAtt();
}
function renderAtt(){
  const cid=$('attCourse').value, date=$('attDate').value; if(!cid||!date) return;
  const course=db.courses.find(x=>x.id===cid);
  const studs=db.students.filter(s=>s.courseId===cid);
  const cyc0 = studs[0] ? null : null;
  $('attTable').innerHTML=`<table><tr><th>Student (age)</th><th>Status on ${date}</th><th>Mark</th></tr>${
    studs.map(s=>{
      const k=s.id+'|'+date, cur=db.attendance[k]||'present';
      const totalExc=excusedCount(s.id,cid);
      return `<tr><td>${s.name} (${calcAge(s.dob)})</td>
        <td><span class="badge ${cur==='present'?'present':cur==='excused'?'excused':'counted'}">${cur.toUpperCase()}</span>
        <small>excused used: ${totalExc}/1</small></td>
        <td><select class="att" onchange="setAtt('${s.id}','${date}',this.value)">
          <option value="present" ${cur==='present'?'selected':''}>Present</option>
          <option value="excused" ${cur==='excused'?'selected':''}>Absent — excused (1/course)</option>
          <option value="counted" ${cur==='counted'?'selected':''}>Absent — counted/paid</option>
        </select></td></tr>`;
    }).join('')||'<tr><td colspan=3>No students in this course</td></tr>'}</table>';

  // payments per current cycle
  $('payTable').innerHTML=`<table><tr><th>Student</th><th>Cycle (4 sessions)</th><th>End</th><th>Paid?</th><th></th></tr>${
    studs.map(s=>{
      const cyc=studentCycle(s,course);
      const paid=isPaid(s.id,cyc.start);
      return `<tr><td>${s.name}</td><td>${fmt(cyc.start)} → ${fmt(cyc.end)}<br><small>${cyc.sessions.join(', ')}</small></td>
        <td>${fmt(cyc.end)}</td>
        <td><span class="badge ${paid?'paid':'unpaid'}">${paid?'PAID':'NOT PAID'}</span></td>
        <td><button onclick="togglePay('${s.id}','${cyc.start}')">${paid?'Mark unpaid':'Mark paid'}</button></td></tr>`;
    }).join('')}</table>`;
}
window.setAtt=(sid,date,val)=>{
  // enforce: only 1 excused per whole course
  if(val==='excused'){
    const s=db.students.find(x=>x.id===sid);
    const otherExc=Object.entries(db.attendance).filter(([k,v])=>k!==sid+'|'+date&&k.startsWith(sid+'|')&&v==='excused').length;
    // check same course? attendance dates belong to this student's course sessions only (we assume)
    if(otherExc>=1){ alert('Already used the 1 excused absence for this course. This will be counted as used.'); val='counted'; }
  }
  db.attendance[sid+'|'+date]=val; save(); renderAll(true);
};
window.togglePay=(sid,cycleStart)=>{
  const k=sid+'|'+cycleStart; db.payments[k]=!db.payments[k]; if(!db.payments[k]) delete db.payments[k];
  save(); renderAll(true);
};

// ---------- Dashboard ----------
function renderDashboard(){
  const totalHThisMonth = db.courses.reduce((a,c)=>{
    const k=new Date().toISOString().slice(0,7);
    const m=monthlyHours(c,12).find(x=>x.month===k);
    return a+(m?m.totalHours:0);
  },0);
  const unpaid=db.students.filter(s=>{ const c=db.courses.find(x=>x.id===s.courseId); if(!c)return false;
    return !isPaid(s.id, studentCycle(s,c).start); }).length;
  $('statsCards').innerHTML=[
    [db.courses.length,'Courses'],[db.students.length,'Students'],
    [totalHThisMonth+'h','Hours this month'],[unpaid,'Unpaid cycles']
  ].map(([v,l])=>`<div class="stat"><b>${v}</b>${l}</div>`).join('');

  $('monthlySummary').innerHTML=db.courses.map(c=>{
    const rows=monthlyHours(c,6).map(m=>`<tr><td>${c.name}</td><td>${m.month}</td><td>${m.sessions} × ${c.hoursPerSession}h</td><td><b>${m.totalHours}h</b></td></tr>`).join('');
    return `<table><tr><th>Course</th><th>Month</th><th>Calc</th><th>Total</th></tr>${rows}</table>`;
  }).join('')||'<p>No data.</p>';

  const rows=db.students.map(s=>{ const c=db.courses.find(x=>x.id===s.courseId); if(!c)return '';
    const cyc=studentCycle(s,c); const dl=daysLeft(cyc.end);
    if(dl>7) return '';
    return `<tr><td>${s.name}</td><td>${c.name}</td><td>${fmt(cyc.start)} → ${fmt(cyc.end)}</td>
      <td>${dl} days</td><td><span class="badge ${isPaid(s.id,cyc.start)?'paid':'unpaid'}">${isPaid(s.id,cyc.start)?'PAID':'NOT PAID'}</span></td></tr>`;
  }).join('');
  $('expiringList').innerHTML=`<table><tr><th>Student</th><th>Course</th><th>Subscription</th><th>Left</th><th>Pay</th></tr>${rows||'<tr><td colspan=5>Nothing expiring.</td></tr>'}</table>`;
}

// ---------- Backup / seed ----------
$('exportBtn').onclick=()=>{
  const a=document.createElement('a');
  a.href=URL.createObjectURL(new Blob([JSON.stringify(db,null,2)],{type:'application/json'}));
  a.download='academy-erp-backup.json'; a.click();
};
$('importFile').onchange=e=>{ const f=e.target.files[0]; if(!f)return;
  const r=new FileReader(); r.onload=()=>{ db=JSON.parse(r.result); save(); renderAll(); }; r.readAsText(f); };
$('wipeBtn').onclick=()=>{ if(confirm('Delete ALL?')){ db={courses:[],students:[],attendance:{},payments:{}}; save(); renderAll(); } };
$('seedBtn').onclick=()=>{
  const c1={id:uid(),name:'English A1',hoursPerSession:2,startDate:'2026-09-07',weekday:1,price:800};
  const c2={id:uid(),name:'Math Kids',hoursPerSession:1.5,startDate:'2026-09-05',weekday:6,price:600};
  db.courses=[c1,c2];
  db.students=[
    {id:uid(),name:'Ahmed Ali',dob:'2015-03-10',phone:'0100000001',courseId:c1.id,subStart:'2026-09-07'},
    {id:uid(),name:'Sara Mohamed',dob:'2012-07-20',phone:'0100000002',courseId:c1.id,subStart:'2026-09-07'},
    {id:uid(),name:'Omar Khaled',dob:'2016-01-05',phone:'0100000003',courseId:c2.id,subStart:'2026-09-05'}];
  save(); renderAll();
};

function renderAll(keepAtt=false){
  renderCourses(); renderStudents(); renderDashboard();
  if(!keepAtt) renderAttDates(); else renderAtt();
  if(!$('attDate').value) renderAttDates();
}
refreshCourseSelects(); renderAll();
