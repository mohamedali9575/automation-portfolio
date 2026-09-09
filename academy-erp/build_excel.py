import openpyxl
from openpyxl.styles import Font, PatternFill, Alignment, Border, Side
from openpyxl.formatting.rule import CellIsRule, FormulaRule
from openpyxl.worksheet.datavalidation import DataValidation
from datetime import date
from copy import copy

OUT = r"D:\Testing\Automation\untitled\academy-erp\academy_erp.xlsx"
wb = openpyxl.Workbook()

hdr_font = Font(bold=True, color="FFFFFF")
hdr_fill = PatternFill("solid", fgColor="1E2A4A")
title_fill = PatternFill("solid", fgColor="FFB020")
thin = Side(style="thin", color="CCCCCC")
border = Border(left=thin, right=thin, top=thin, bottom=thin)

def style_header(ws, row=1, maxcol=20):
    for c in range(1, maxcol+1):
        cell = ws.cell(row=row, column=c)
        if cell.value:
            cell.font = hdr_font; cell.fill = hdr_fill
            cell.alignment = Alignment(horizontal="center", wrap_text=True)
            cell.border = border

def autofit(ws, widths):
    for i,w in enumerate(widths, start=1):
        ws.column_dimensions[openpyxl.utils.get_column_letter(i)].width = w

# ---------- README ----------
ws = wb.active; ws.title = "README"
ws["A1"] = "Academy ERP — Excel (dynamic formulas)"
ws["A1"].font = Font(bold=True, size=16)
rules = [
 "1) COURSES: fill Course_Name, Hours_Per_Session (set once at start), Start_Date (first session), Weekday, Price_Per_4.",
 "2) SESSIONS sheet auto-generates 1 session/week: Date = Start_Date + (Session#-1)*7. Change course start -> all dates update.",
 "3) STUDENTS: fill Name, DOB (Age auto =DATEDIF), Phone, Course_Name (dropdown), Sub_Start. Rest is AUTO.",
 "4) SUBSCRIPTION = pay every 4 sessions. Sub_End = Sub_Start + 21 days. +7 more if 1 excused absence used.",
 "   Example: start 2026-09-07 -> end 2026-09-28. With 1 excused -> end 2026-10-05.",
 "5) ATTENDANCE: log Student_Name + Session_Date + Status (Present/Excused/Counted). Only ONE Excused per student per course counts — second Excused is treated as Counted (see column E).",
 "6) MONTHLY totals: Sessions!D month + Monthly_Hours COUNTIFS x Hours. DASHBOARD sums current month via SUMIFS + TODAY().",
 "7) PAY: Students!L = Paid/Unpaid dropdown. Dashboard counts unpaid + expiring (Days_Left<=7).",
]
for i,r in enumerate(rules, start=3): ws.cell(row=i, column=1, value=r)
ws.column_dimensions["A"].width = 140

# ---------- COURSES ----------
wc = wb.create_sheet("Courses")
wc.append(["CourseID","Course_Name","Hours_Per_Session","Start_Date","Weekday","Price_Per_4"])
wc.append(["C1","English A1",2,date(2026,9,7),"Monday",800])
wc.append(["C2","Math Kids",1.5,date(2026,9,5),"Saturday",600])
wc.append(["C3","","","","",""])  # blank row for user
for r in wc["D2:D4"]: 
    for c in r: c.number_format = "YYYY-MM-DD"
wc["C2"].number_format='0.0'; wc["C3"].number_format='0.0'
style_header(wc, maxcol=6); autofit(wc,[10,20,18,14,12,14])
wc.freeze_panes="A2"; wc.auto_filter.ref="A1:F4"

# ---------- STUDENTS ----------
st = wb.create_sheet("Students")
st.append(["ID","Student_Name","DOB","Age_auto","Phone","Course_Name","Sub_Start",
 "Hours_auto","Excused_Used","Sub_End_auto","Days_Left_auto","Pay_Status","Present_Count","Next_Due"])
demo = [
 ["S1","Ahmed Ali",date(2015,3,10),"","0100000001","English A1",date(2026,9,7)],
 ["S2","Sara Mohamed",date(2012,7,20),"","0100000002","English A1",date(2026,9,7)],
 ["S3","Omar Khaled",date(2016,1,5),"","0100000003","Math Kids",date(2026,9,5)],
]
for r in demo: st.append(r)
for _ in range(17): st.append(["","","","","","",""])  # 20 total entry rows
# formulas rows 2..21
for row in range(2,22):
    st.cell(row=row,column=4).value  = f'=IF(C{row}="","",DATEDIF(C{row},TODAY(),"Y"))'
    st.cell(row=row,column=8).value  = f'=IF(F{row}="","",VLOOKUP(F{row},Courses!$B:$C,2,FALSE))'
    st.cell(row=row,column=9).value  = f'=IF(B{row}="","",COUNTIFS(Attendance!$A:$A,B{row},Attendance!$D:$D,"Excused"))'
    st.cell(row=row,column=10).value = f'=IF(G{row}="","",G{row}+21+7*(I{row}>=1))'
    st.cell(row=row,column=11).value = f'=IF(J{row}="","",J{row}-TODAY())'
    st.cell(row=row,column=13).value = f'=IF(B{row}="","",COUNTIFS(Attendance!$A:$A,B{row},Attendance!$D:$D,"Present"))'
    st.cell(row=row,column=14).value = f'=IF(J{row}="","",J{row}+7)'
    if row>=5: st.cell(row=row,column=12).value = None
for row in (2,3,4): st.cell(row=row,column=12).value="Unpaid"
for r in st["C2:C21"]:
    for c in r: c.number_format="YYYY-MM-DD"
for r in st["G2:G21"]:
    for c in r: c.number_format="YYYY-MM-DD"
for r in st["J2:J21"]:
    for c in r: c.number_format="YYYY-MM-DD"
for r in st["N2:N21"]:
    for c in r: c.number_format="YYYY-MM-DD"
style_header(st, maxcol=14); autofit(st,[8,18,12,10,14,16,12,12,13,13,12,12,14,12])
st.freeze_panes="A2"; st.auto_filter.ref="A1:N21"
# validations
dv_course = DataValidation(type="list", formula1="Courses!$B$2:$B$50", allow_blank=True)
dv_course.sqref="F2:F21"; st.add_data_validation(dv_course)
dv_pay = DataValidation(type="list", formula1='"Paid,Unpaid"', allow_blank=True)
dv_pay.sqref="L2:L21"; st.add_data_validation(dv_pay)
# conditional formatting
red=PatternFill("solid",fgColor="FFC7CE"); green=PatternFill("solid",fgColor="C6EFCE")
yel=PatternFill("solid",fgColor="FFEB9C")
st.conditional_formatting.add("L2:L21", FormulaRule(formula=['$L2="Unpaid"'], fill=red))
st.conditional_formatting.add("L2:L21", FormulaRule(formula=['$L2="Paid"'], fill=green))
st.conditional_formatting.add("K2:K21", CellIsRule(operator="lessThan", formula=["0"], fill=red))
st.conditional_formatting.add("K2:K21", CellIsRule(operator="between", formula=["0","7"], fill=yel))
st.conditional_formatting.add("I2:I21", CellIsRule(operator="greaterThan", formula=["1"], fill=red))

# ---------- SESSIONS ----------
ss = wb.create_sheet("Sessions")
ss.append(["Course_Name","Session_#","Session_Date_auto","Month_auto","Hours_auto"])
courses = ["English A1","Math Kids"]
N=30
row=2
for c in courses:
    for n in range(1,N+1):
        ss.cell(row=row,column=1,value=c)
        ss.cell(row=row,column=2,value=n)
        ss.cell(row=row,column=3,value=f'=VLOOKUP(A{row},Courses!$B:$D,3,FALSE)+(B{row}-1)*7')
        ss.cell(row=row,column=4,value=f'=IF(C{row}="","",TEXT(C{row},"yyyy-mm"))')
        ss.cell(row=row,column=5,value=f'=VLOOKUP(A{row},Courses!$B:$C,2,FALSE)')
        row+=1
for r in ss["C2:C61"]:
    for c in r: c.number_format="YYYY-MM-DD"
style_header(ss, maxcol=5); autofit(ss,[18,10,16,12,12])
ss.freeze_panes="A2"; ss.auto_filter.ref=f"A1:E{row-1}"

# ---------- ATTENDANCE ----------
at = wb.create_sheet("Attendance")
at.append(["Student_Name","Session_Date","Course_auto","Status","Counts_As_Used","Note"])
at.append(["Ahmed Ali",date(2026,9,14),"","Excused"])
at.append(["Sara Mohamed",date(2026,9,14),"","Present"])
at.append(["Ahmed Ali",date(2026,9,21),"","Counted"])
for _ in range(27): at.append(["","","","","",""])
for row in range(2,32):
    at.cell(row=row,column=3).value=f'=IF(A{row}="","",VLOOKUP(A{row},Students!$B:$F,5,FALSE))'
    at.cell(row=row,column=5).value=f'=IF(D{row}="Excused","NO (extends once)",IF(D{row}="","", "YES"))'
    at.cell(row=row,column=6).value=f'=IF(COUNTIFS($A$2:$A$31,A{row},$D$2:$D$31,"Excused")>1,"WARNING: >1 Excused — extras count as used","")'
for r in at["B2:B31"]:
    for c in r: c.number_format="YYYY-MM-DD"
style_header(at, maxcol=6); autofit(at,[18,14,18,12,20,42])
at.freeze_panes="A2"; at.auto_filter.ref="A1:F31"
dv_st = DataValidation(type="list", formula1="Students!$B$2:$B$21", allow_blank=True)
dv_st.sqref="A2:A31"; at.add_data_validation(dv_st)
dv_status = DataValidation(type="list", formula1='"Present,Excused,Counted"', allow_blank=True)
dv_status.sqref="D2:D31"; at.add_data_validation(dv_status)
at.conditional_formatting.add("D2:D31", FormulaRule(formula=['$D2="Present"'], fill=green))
at.conditional_formatting.add("D2:D31", FormulaRule(formula=['$D2="Excused"'], fill=yel))
at.conditional_formatting.add("D2:D31", FormulaRule(formula=['$D2="Counted"'], fill=red))

# ---------- MONTHLY_HOURS ----------
mh = wb.create_sheet("Monthly_Hours")
mh.append(["Course_Name","Month_yyyy_mm","Sessions_Count_auto","Total_Hours_auto"])
months=["2026-09","2026-10","2026-11","2026-12","2027-01","2027-02"]
r=2
for c in courses+[""]:
    for m in months:
        if c=="":
            mh.cell(row=r,column=1,value=""); mh.cell(row=r,column=2,value="")
        else:
            mh.cell(row=r,column=1,value=c); mh.cell(row=r,column=2,value=m)
        mh.cell(row=r,column=3,value=f'=IF(OR(A{r}="",B{r}=""),"",COUNTIFS(Sessions!$A:$A,A{r},Sessions!$D:$D,B{r}))')
        mh.cell(row=r,column=4,value=f'=IF(OR(A{r}="",B{r}=""),"",C{r}*VLOOKUP(A{r},Courses!$B:$C,2,FALSE))')
        r+=1
style_header(mh, maxcol=4); autofit(mh,[18,16,18,16])

# ---------- DASHBOARD ----------
db = wb.create_sheet("Dashboard")
db["A1"]="ACADEMY DASHBOARD (all live formulas)"; db["A1"].font=Font(bold=True,size=14)
items=[
 ("Total courses","=COUNTA(Courses!B2:B50)"),
 ("Total students","=COUNTA(Students!B2:B21)"),
 ("Hours this month (all courses)",'=SUMIFS(Monthly_Hours!D:D,Monthly_Hours!B:B,TEXT(TODAY(),"yyyy-mm"))'),
 ("Unpaid cycles",'=COUNTIF(Students!L:L,"Unpaid")'),
 ("Expiring in <=7 days (incl. expired)",'=COUNTIFS(Students!K:K,"<=7",Students!B:B,"<>")'),
 ("Excused absences logged",'=COUNTIF(Attendance!D:D,"Excused")'),
 ("Total sessions scheduled","=COUNTA(Sessions!C:C)"),
]
for i,(label,f) in enumerate(items, start=3):
    db.cell(row=i,column=1,value=label).font=Font(bold=True)
    db.cell(row=i,column=2,value=f).font=Font(bold=True,size=12)
db["A11"]="Expiring list (auto): students with Days_Left<=7"
db["A12"]="Student"; db["B12"]="Course"; db["C12"]="Sub_End"; db["D12"]="Days_Left"; db["E12"]="Pay"
for c in ["A12","B12","C12","D12","E12"]:
    db[c].font=hdr_font; db[c].fill=hdr_fill
for row in range(13,28):
    # show student name only if expiring, else blank
    db.cell(row=row,column=1,value=f'=IF(ROW()-12>COUNTA(Students!$B:$B),"",IF(INDEX(Students!$K:$K,ROW()-12+1)<=7,INDEX(Students!$B:$B,ROW()-12+1),""))')
    db.cell(row=row,column=2,value=f'=IF(A{row}="","",VLOOKUP(A{row},Students!$B:$F,5,FALSE))')
    db.cell(row=row,column=3,value=f'=IF(A{row}="","",VLOOKUP(A{row},Students!$B:$J,9,FALSE))')
    db.cell(row=row,column=4,value=f'=IF(A{row}="","",VLOOKUP(A{row},Students!$B:$K,10,FALSE))')
    db.cell(row=row,column=5,value=f'=IF(A{row}="","",VLOOKUP(A{row},Students!$B:$L,11,FALSE))')
    db.cell(row=row,column=3).number_format="YYYY-MM-DD"
autofit(db,[32,22,14,12,12])

wb.save(OUT)
print("saved", OUT)
