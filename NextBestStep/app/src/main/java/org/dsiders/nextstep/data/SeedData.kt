package org.dsiders.nextstep.data

import androidx.room.withTransaction

/**
 * Sample content so the app is usable on first launch. Everything here mirrors the
 * sample data in the Figma prototype; Barabari staff should review the roles and
 * requirements from the Jobs tab before using them with real students.
 */
object SeedData {
    const val DEMO_PASSWORD = "barabari123"
    private const val HOUR = 3_600_000L
    private const val DAY = 24 * HOUR

    suspend fun seedIfEmpty(db: AppDatabase) {
        if (db.users().count() > 0) return
        db.withTransaction { seed(db) }
    }

    private suspend fun seed(db: AppDatabase) {
        val now = System.currentTimeMillis()

        // --- Caseworkers -------------------------------------------------------
        val ravi = db.users().insert(
            user(
                UserRole.CASEWORKER, "Ravi Teja", "ravi",
                centre = "Kukatpally centre", languages = "Telugu, Hindi, English",
                topics = "Resumes, Choosing a course, Interview practice, Scholarships",
                phone = "",
            )
        )
        db.users().insert(
            user(
                UserRole.CASEWORKER, "Lakshmi Prasanna", "lakshmi",
                centre = "Ameerpet centre", languages = "Telugu, English",
                topics = "Government jobs, Documents, Scholarships", available = false,
            )
        )

        // --- Roles and their requirement graphs --------------------------------
        val data = db.roles().upsert(
            RoleEntity(
                title = "Data analyst", field = "Working with data and numbers",
                description = "Turns raw numbers into answers a team can act on.",
                keywords = "data,analyst,analytics,excel,sql,numbers,statistics,dashboard,business analyst",
            )
        )
        val sheets = req(db, data, "The basics of spreadsheets", RequirementKind.SKILL, 0, action = "Learn spreadsheet basics",
            why = "Almost every data job starts in Excel or Google Sheets.",
            time = "About 2 weeks of practice",
            need = "A phone or computer with Google Sheets",
            where = "Barabari digital skills module, any centre",
            how = "Learn sorting, filtering, SUM/AVERAGE, and one pivot table. Practise on your own expenses for a week.",
            hint = "Ask whether they have used Excel or Google Sheets for anything, even college work.")
        val resume = req(db, data, "A resume", RequirementKind.ARTEFACT, 1, action = "Write your first resume",
            why = "Most courses and interviews ask for one.",
            time = "About 2 hours",
            need = "Your marksheets\nAny projects or coursework\nA phone number and email",
            where = "Barabari resume clinic, Kukatpally centre, Saturdays 11 AM",
            how = "Use a one-page template. Put education first, then skills you can show, then projects. Ask a mentor to read it once.",
            hint = "Ask whether they have a resume; a half-finished one counts as started.")
        val sql = req(db, data, "Basic SQL", RequirementKind.SKILL, 2, action = "Learn basic SQL",
            why = "Every data analyst role expects it. Free courses exist.",
            time = "Free online, about 3 weeks",
            need = "A laptop or a phone with a browser\n30 minutes a day",
            where = "Free SQL course links from your Barabari mentor",
            how = "Learn SELECT, WHERE, GROUP BY and JOIN. Do 20 practice questions on a free practice site.",
            depends = listOf(sheets))
        req(db, data, "Build one small project", RequirementKind.ARTEFACT, 3,
            why = "Something you can show someone in an interview.",
            time = "About 2 weekends",
            need = "A free public dataset\nYour SQL or spreadsheet skills",
            where = "Ask your mentor for a project idea",
            how = "Pick a question you care about, find a public dataset, answer it with a chart and three sentences.",
            depends = listOf(sql))
        req(db, data, "Python for data analysis", RequirementKind.SKILL, 4, priority = Priority.LATER,
            why = "Useful later, but not needed for a first role.")
        req(db, data, "Dashboards in Power BI or Tableau", RequirementKind.SKILL, 5, priority = Priority.LATER,
            why = "Nice to have once you're working with data every day.")
        req(db, data, "Statistics beyond the basics", RequirementKind.SKILL, 6, priority = Priority.LATER,
            why = "Comes with time on the job.")

        val web = db.roles().upsert(
            RoleEntity(
                title = "Web developer", field = "Building websites or apps",
                description = "Builds websites and simple web apps.",
                keywords = "web,website,developer,coding,code,programming,app,html,css,javascript,software,frontend",
            )
        )
        val html = req(db, web, "HTML and CSS basics", RequirementKind.SKILL, 0, action = "Learn HTML and CSS basics",
            why = "Every website is built on these two.",
            time = "About 3 weeks",
            need = "A laptop, or a phone with a code editor app",
            where = "Barabari coding club, Thursdays",
            how = "Build a one-page site about yourself. Then copy the layout of a site you like.")
        req(db, web, "A resume", RequirementKind.ARTEFACT, 1, action = "Write your first resume",
            why = "Internships and courses ask for one.",
            time = "About 2 hours",
            need = "Your marksheets\nAny projects or coursework\nA phone number and email",
            where = "Barabari resume clinic, Kukatpally centre, Saturdays 11 AM")
        val js = req(db, web, "JavaScript basics", RequirementKind.SKILL, 2, action = "Learn JavaScript basics",
            why = "Needed to make a page do anything.",
            time = "About 4 weeks",
            need = "A laptop\n30 minutes a day",
            where = "Free JavaScript course links from your mentor",
            depends = listOf(html))
        req(db, web, "A GitHub profile with two projects", RequirementKind.ARTEFACT, 3, action = "Put two projects on GitHub",
            why = "This is what people look at before they call you.",
            time = "About 2 weekends",
            need = "Your projects\nAn email address",
            where = "Barabari coding club, Thursdays",
            depends = listOf(js))
        req(db, web, "A framework like React", RequirementKind.SKILL, 4, priority = Priority.LATER,
            why = "Learn it after JavaScript feels comfortable.")
        req(db, web, "Back-end and databases", RequirementKind.SKILL, 5, priority = Priority.LATER,
            why = "Not needed for a first front-end role.")

        val design = db.roles().upsert(
            RoleEntity(
                title = "Graphic designer", field = "Design and creative work",
                description = "Designs posters, social media posts and simple app screens.",
                keywords = "design,designer,graphic,creative,art,canva,figma,ui,ux,drawing,poster",
            )
        )
        val tool = req(db, design, "Comfort with one design tool", RequirementKind.SKILL, 0, action = "Get comfortable with Canva or Figma",
            why = "Canva or Figma is where the work happens.",
            time = "About 2 weeks",
            need = "A phone or laptop with Canva or Figma (both free)",
            where = "Barabari creative skills session, Wednesdays",
            how = "Recreate three posters you like, exactly. Then change one thing in each.")
        req(db, design, "A resume", RequirementKind.ARTEFACT, 1, action = "Write your first resume",
            why = "Studios and courses still ask for one.",
            time = "About 2 hours",
            need = "Your marksheets\nA phone number and email",
            where = "Barabari resume clinic, Kukatpally centre, Saturdays 11 AM")
        req(db, design, "A portfolio of three pieces", RequirementKind.ARTEFACT, 2, action = "Put together three portfolio pieces",
            why = "Designers are hired on what they can show.",
            time = "About 3 weekends",
            need = "Three finished designs\nA free portfolio page or PDF",
            where = "Ask your mentor for a review",
            depends = listOf(tool))
        req(db, design, "Motion and video editing", RequirementKind.SKILL, 3, priority = Priority.LATER,
            why = "Useful later; not needed to start.")

        val gov = db.roles().upsert(
            RoleEntity(
                title = "Government job (state exams)", field = "Government jobs",
                description = "Clerical and assistant posts filled through state recruitment exams.",
                keywords = "government,govt,sarkari,exam,tspsc,group,clerk,public service,police,railway,bank",
            )
        )
        val idDocs = req(db, gov, "Aadhaar with your mobile number linked", RequirementKind.DOCUMENT, 0, action = "Link your mobile number to Aadhaar",
            why = "Online exam registration sends codes to this number.",
            time = "One visit, about 1 hour",
            need = "Your Aadhaar card\nYour mobile phone",
            where = "Nearest Aadhaar enrolment centre or post office")
        val domicile = req(db, gov, "Local status (domicile) certificate", RequirementKind.DOCUMENT, 1, action = "Apply for your local status certificate",
            why = "State posts usually ask for proof that you are a local candidate.",
            time = "About 1–2 weeks after applying",
            need = "Study certificates for the required years\nAadhaar card\nTwo passport photos",
            where = "Your nearest MeeSeva centre; ask your mentor which form applies",
            depends = listOf(idDocs))
        req(db, gov, "One-time registration on the recruitment website", RequirementKind.DOCUMENT, 2, action = "Do your one-time registration",
            why = "You can only apply for posts once this is done.",
            time = "About 1 hour",
            need = "Aadhaar\nScanned photo and signature\nYour certificates",
            where = "Any internet centre, or Barabari centre computers",
            depends = listOf(domicile))
        req(db, gov, "A study plan for the exam syllabus", RequirementKind.SKILL, 3, action = "Make a study plan for the syllabus",
            why = "These exams are competitive; a plan beats random reading.",
            time = "Ongoing, a few months",
            need = "The latest syllabus\nPrevious year papers",
            where = "Barabari study circle")
        req(db, gov, "Coaching or test series", RequirementKind.EXPERIENCE, 4, priority = Priority.LATER,
            why = "Helpful closer to the exam date.")

        // --- Openings (sample) -------------------------------------------------
        db.openings().upsert(
            OpeningEntity(
                roleId = data, title = "Data trainee (sample)", organisation = "Sample analytics firm",
                location = "Hyderabad", deadline = "30 Nov",
                description = "Six-month paid traineeship. Freshers welcome.",
                howToApply = "Send your resume through your Barabari mentor.", createdBy = ravi,
            )
        )
        db.openings().upsert(
            OpeningEntity(
                roleId = web, title = "Junior web developer intern (sample)", organisation = "Sample startup",
                location = "Remote", deadline = "15 Dec",
                description = "Three-month internship building landing pages.",
                howToApply = "Share your GitHub profile with your mentor.", createdBy = ravi,
            )
        )

        // --- Sample students ---------------------------------------------------
        val priya = db.users().insert(
            user(
                UserRole.STUDENT, "Priya Komati", "priya", college = "Govt Degree College, Kukatpally",
                course = "B.Sc Computer Science", year = "Final year", district = "Hyderabad", mentorId = ravi,
            )
        )
        goal(db, priya, "I want to become a data analyst but I don't know where to start", data,
            "Working with data and numbers", "You want to work with data", now - 2 * HOUR)
        fact(db, priya, "Studying", "B.Sc Computer Science")
        fact(db, priya, "Year", "Final year")
        fact(db, priya, "District", "Hyderabad")
        fact(db, priya, "Used Excel", "Yes, in college")
        status(db, priya, sheets, ReqStatus.HAVE, 0.85f, "You said you've used Excel in college.", now - 2 * HOUR)
        status(db, priya, resume, ReqStatus.MISSING, 0.9f, "You said you don't have one yet.", now - 2 * HOUR)
        status(db, priya, sql, ReqStatus.MISSING, 0.5f, "You haven't mentioned SQL so far.", now - 2 * HOUR)
        db.helpRequests().upsert(
            HelpRequestEntity(studentId = priya, caseworkerId = ravi, reason = "Stuck: no resume yet",
                message = "I don't know how to start my resume.", createdAt = now - 2 * HOUR)
        )

        val sana = db.users().insert(user(UserRole.STUDENT, "Sana Begum", "sana", course = "B.Com", year = "Final year", district = "Hyderabad"))
        goal(db, sana, "I don't know what I want to do after college", null, "", "You're not sure yet, and that's fine", now - 5 * HOUR, done = false)
        db.helpRequests().upsert(
            HelpRequestEntity(studentId = sana, caseworkerId = ravi, reason = "Asked to talk to someone",
                message = "Can someone help me choose?", createdAt = now - 5 * HOUR)
        )

        val arjun = db.users().insert(user(UserRole.STUDENT, "Arjun Mehta", "arjun", course = "B.Sc Maths", year = "Final year", district = "Medchal"))
        goal(db, arjun, "I want to build websites", web, "Building websites or apps", "You want to build websites", now - DAY)
        status(db, arjun, html, ReqStatus.PARTIAL, 0.8f, "You said you're learning HTML.", now - DAY)

        val kiran = db.users().insert(user(UserRole.STUDENT, "Kiran Rao", "kiran", course = "B.A.", year = "Final year", district = "Rangareddy"))
        goal(db, kiran, "I want a government job", gov, "Government jobs", "You want a government job", now - 2 * DAY)
        status(db, kiran, idDocs, ReqStatus.HAVE, 0.9f, "You said your Aadhaar is linked to your phone.", now - 2 * DAY)
        status(db, kiran, domicile, ReqStatus.MISSING, 0.9f, "You said you don't have a local status certificate.", now - 2 * DAY)

        val divya = db.users().insert(user(UserRole.STUDENT, "Divya Nair", "divya", course = "B.F.A.", year = "Second year", district = "Hyderabad"))
        goal(db, divya, "I want to become a designer", design, "Design and creative work", "You want to do design work", now - 4 * DAY)
        status(db, divya, tool, ReqStatus.MISSING, 0.8f, "You said you haven't used Canva or Figma.", now - 4 * DAY)

        val mahesh = db.users().insert(user(UserRole.STUDENT, "Mahesh Goud", "mahesh", course = "B.Sc Statistics", year = "Final year", district = "Hyderabad"))
        goal(db, mahesh, "I want to work with data", data, "Working with data and numbers", "You want to work with data", now - 6 * DAY)
        status(db, mahesh, sheets, ReqStatus.HAVE, 0.9f, "You said you use spreadsheets often.", now - 6 * DAY)
        status(db, mahesh, resume, ReqStatus.HAVE, 0.9f, "You finished your resume.", now - 6 * DAY)
    }

    private fun user(
        role: UserRole, name: String, username: String,
        college: String = "", course: String = "", year: String = "", district: String = "",
        mentorId: Long? = null, centre: String = "", languages: String = "English",
        topics: String = "", available: Boolean = true, phone: String = "",
    ): UserEntity {
        val salt = Passwords.newSalt()
        return UserEntity(
            role = role, fullName = name, username = username,
            passwordHash = Passwords.hash(DEMO_PASSWORD, salt), salt = salt,
            college = college, course = course, year = year, district = district, mentorId = mentorId,
            centre = centre, languages = languages, topics = topics, available = available, phone = phone,
        )
    }

    private suspend fun req(
        db: AppDatabase, roleId: Long, name: String, kind: RequirementKind, order: Int,
        why: String = "", time: String = "", need: String = "", where: String = "", how: String = "",
        depends: List<Long> = emptyList(), priority: Priority = Priority.CORE, hint: String = "", action: String = "",
    ): Long = db.requirements().upsert(
        RequirementEntity(
            roleId = roleId, name = name, actionTitle = action, kind = kind, priority = priority, orderIndex = order,
            whyNeeded = why, timeEstimate = time, whatYouNeed = need, whereToGetHelp = where,
            howTo = how, dependsOn = depends.joinToString(","), questionHint = hint,
        )
    )

    private suspend fun goal(
        db: AppDatabase, studentId: Long, text: String, roleId: Long?, field: String,
        readback: String, at: Long, done: Boolean = true,
    ) = db.goals().upsert(
        GoalEntity(studentId, text, roleId, field, readback, intakeDone = done, updatedAt = at)
    )

    private suspend fun fact(db: AppDatabase, studentId: Long, label: String, value: String) =
        db.facts().upsert(FactEntity(studentId = studentId, label = label, value = value))

    private suspend fun status(
        db: AppDatabase, studentId: Long, reqId: Long, status: ReqStatus, conf: Float, evidence: String, at: Long,
    ) = db.reqStatus().upsert(
        ReqStatusEntity(studentId, reqId, status, conf, evidence, Source.STUDENT, at)
    )
}
