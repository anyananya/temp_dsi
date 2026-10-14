package org.dsiders.nextstep.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MarkEmailUnread
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.flow.flowOf
import org.dsiders.nextstep.AppContainer
import org.dsiders.nextstep.data.RequestStatus
import org.dsiders.nextstep.data.UserEntity
import org.dsiders.nextstep.data.UserRole
import org.dsiders.nextstep.ui.auth.LoginScreen
import org.dsiders.nextstep.ui.auth.RegisterScreen
import org.dsiders.nextstep.ui.caseworker.AddStudentScreen
import org.dsiders.nextstep.ui.caseworker.CaseworkerProfileScreen
import org.dsiders.nextstep.ui.caseworker.JobsScreen
import org.dsiders.nextstep.ui.caseworker.OpeningEditorScreen
import org.dsiders.nextstep.ui.caseworker.RequestsScreen
import org.dsiders.nextstep.ui.caseworker.RoleEditorScreen
import org.dsiders.nextstep.ui.caseworker.StudentDetailScreen
import org.dsiders.nextstep.ui.caseworker.StudentsScreen
import org.dsiders.nextstep.ui.components.LoadingBlock
import org.dsiders.nextstep.ui.student.HowToScreen
import org.dsiders.nextstep.ui.student.NextStepScreen
import org.dsiders.nextstep.ui.student.OpeningDetailScreen
import org.dsiders.nextstep.ui.student.PathScreen
import org.dsiders.nextstep.ui.student.QuestionScreen
import org.dsiders.nextstep.ui.student.ReadbackScreen
import org.dsiders.nextstep.ui.student.StandingScreen
import org.dsiders.nextstep.ui.student.StudentHelpScreen
import org.dsiders.nextstep.ui.student.StudentHomeScreen
import org.dsiders.nextstep.ui.student.StudentProfileScreen
import org.dsiders.nextstep.ui.theme.Palette

object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"

    const val S_HOME = "s/home"
    const val S_HELP = "s/help"
    const val S_PROFILE = "s/profile"
    const val S_OPENING = "s/opening/{oid}"
    fun sOpening(id: Long) = "s/opening/$id"

    // The student flow takes the student's id so a caseworker can run it with them.
    const val READBACK = "flow/readback/{sid}"
    const val QUESTION = "flow/question/{sid}"
    const val STANDING = "flow/standing/{sid}"
    const val NEXT = "flow/next/{sid}"
    const val PATH = "flow/path/{sid}"
    const val HOWTO = "flow/howto/{sid}/{rid}"
    fun readback(sid: Long) = "flow/readback/$sid"
    fun question(sid: Long) = "flow/question/$sid"
    fun standing(sid: Long) = "flow/standing/$sid"
    fun next(sid: Long) = "flow/next/$sid"
    fun path(sid: Long) = "flow/path/$sid"
    fun howTo(sid: Long, rid: Long) = "flow/howto/$sid/$rid"

    const val C_STUDENTS = "c/students"
    const val C_REQUESTS = "c/requests"
    const val C_JOBS = "c/jobs"
    const val C_PROFILE = "c/profile"
    const val C_STUDENT = "c/student/{sid}"
    const val C_ADD_STUDENT = "c/addstudent"
    const val C_ROLE = "c/role/{rid}"
    const val C_OPENING = "c/opening/{oid}"
    fun cStudent(sid: Long) = "c/student/$sid"
    fun cRole(rid: Long) = "c/role/$rid"
    fun cOpening(oid: Long) = "c/opening/$oid"
}

private data class Tab(val route: String, val label: String, val icon: ImageVector, val selectedIcon: ImageVector)

@Composable
fun AppRoot(container: AppContainer) {
    val ready by container.ready.collectAsState()
    val userId by container.session.userId.collectAsState()
    if (!ready) {
        Box(Modifier.fillMaxSize()) { LoadingBlock("Getting things ready…") }
        return
    }
    val me by remember(userId) { userId?.let { container.repo.observeUser(it) } ?: flowOf(null) }.collectAsState(initial = null)
    val signedIn = userId != null

    // A fresh navigation graph per signed-in person, so roles never share a back stack.
    key(userId, me?.role) {
        val nav = rememberNavController()
        val current = me
        when {
            !signedIn -> AuthGraph(container, nav)
            current == null -> Box(Modifier.fillMaxSize()) { LoadingBlock("Signing you in…") }
            else -> RoleScaffold(container, nav, current)
        }
    }
}

@Composable
private fun AuthGraph(container: AppContainer, nav: NavHostController) {
    NavHost(nav, startDestination = Routes.LOGIN) {
        composable(Routes.LOGIN) { LoginScreen(container, onCreateAccount = { nav.navigate(Routes.REGISTER) }) }
        composable(Routes.REGISTER) { RegisterScreen(container, onBackToLogin = { nav.popBackStack() }) }
    }
}

@Composable
private fun RoleScaffold(container: AppContainer, nav: NavHostController, me: UserEntity) {
    val isStudent = me.role == UserRole.STUDENT
    val meState = rememberUpdatedState(me)
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route.orEmpty()

    val requests by remember { container.repo.observeRequests() }.collectAsState(initial = emptyList())
    val newRequests = requests.count { it.status == RequestStatus.NEW && (it.caseworkerId == null || it.caseworkerId == me.id) }

    val tabs = if (isStudent) listOf(
        Tab(Routes.S_HOME, "Home", Icons.Outlined.Home, Icons.Filled.Home),
        Tab(Routes.path(me.id), "My path", Icons.Outlined.Flag, Icons.Filled.Flag),
        Tab(Routes.S_HELP, "Help", Icons.Outlined.ChatBubbleOutline, Icons.AutoMirrored.Outlined.HelpOutline),
        Tab(Routes.S_PROFILE, "Profile", Icons.Outlined.Person, Icons.Filled.Person),
    ) else listOf(
        Tab(Routes.C_STUDENTS, "Students", Icons.Outlined.Groups, Icons.Filled.Groups),
        Tab(Routes.C_REQUESTS, "Requests", Icons.Outlined.MarkEmailUnread, Icons.AutoMirrored.Outlined.Assignment),
        Tab(Routes.C_JOBS, "Jobs", Icons.Outlined.WorkOutline, Icons.Filled.Work),
        Tab(Routes.C_PROFILE, "Profile", Icons.Outlined.Person, Icons.Filled.Person),
    )

    val selected = if (isStudent) when {
        route.startsWith("flow/standing") || route.startsWith("flow/next") || route.startsWith("flow/path") || route.startsWith("flow/howto") -> 1
        route == Routes.S_HELP -> 2
        route == Routes.S_PROFILE -> 3
        else -> 0
    } else when {
        route == Routes.C_REQUESTS -> 1
        route == Routes.C_JOBS || route.startsWith("c/role") || route.startsWith("c/opening") -> 2
        route == Routes.C_PROFILE -> 3
        else -> 0
    }

    Scaffold(
        containerColor = Palette.Page,
        bottomBar = {
            NavigationBar(containerColor = Color.White, tonalElevation = 0.dp) {
                tabs.forEachIndexed { index, tab ->
                    val isSel = index == selected
                    NavigationBarItem(
                        selected = isSel,
                        onClick = {
                            if (!isSel) nav.navigate(tab.route) {
                                popUpTo(nav.graph.startDestinationId) { saveState = false }
                                launchSingleTop = true
                            }
                        },
                        icon = {
                            if (!isStudent && index == 1 && newRequests > 0) {
                                BadgedBox(badge = { Badge(containerColor = Palette.Orange) { Text("$newRequests") } }) {
                                    Icon(if (isSel) tab.selectedIcon else tab.icon, null)
                                }
                            } else Icon(if (isSel) tab.selectedIcon else tab.icon, null)
                        },
                        label = { Text(tab.label, style = MaterialTheme.typography.labelMedium) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Palette.Navy, selectedTextColor = Palette.Navy,
                            unselectedIconColor = Palette.Grey, unselectedTextColor = Palette.Grey,
                            indicatorColor = Color.Transparent,
                        ),
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding)) {
            // Built once per role. Screens read the latest profile through [meState], so editing a
            // profile never rebuilds the graph (which would reset the back stack).
            val graph: NavGraphBuilder.() -> Unit = remember(isStudent) {
                {
                    flowGraph(container, nav, meState)
                    if (isStudent) studentGraph(container, nav, meState) else caseworkerGraph(container, nav, meState)
                }
            }
            NavHost(nav, startDestination = if (isStudent) Routes.S_HOME else Routes.C_STUDENTS, builder = graph)
        }
    }
}

private val sid = navArgument("sid") { type = NavType.LongType }

private fun NavGraphBuilder.flowGraph(container: AppContainer, nav: NavHostController, meState: State<UserEntity>) {
    composable(Routes.READBACK, listOf(sid)) { entry ->
        val studentId = entry.arguments!!.getLong("sid")
        ReadbackScreen(container, studentId,
            onQuestion = { nav.navigate(Routes.question(studentId)) { popUpTo(Routes.READBACK) { inclusive = true } } },
            onDone = { nav.navigate(Routes.standing(studentId)) { popUpTo(Routes.READBACK) { inclusive = true } } },
        )
    }
    composable(Routes.QUESTION, listOf(sid)) { entry ->
        val studentId = entry.arguments!!.getLong("sid")
        QuestionScreen(container, studentId,
            onDone = { nav.navigate(Routes.standing(studentId)) { popUpTo(Routes.QUESTION) { inclusive = true } } },
            onExit = { nav.popBackStack() },
        )
    }
    composable(Routes.STANDING, listOf(sid)) { entry ->
        val studentId = entry.arguments!!.getLong("sid")
        StandingScreen(container, meState.value, studentId,
            onNext = { nav.navigate(Routes.next(studentId)) },
            onBack = { if (!nav.popBackStack()) nav.navigate(Routes.S_HOME) },
            onRestart = { nav.navigate(if (meState.value.role == UserRole.STUDENT) Routes.S_HOME else Routes.cStudent(studentId)) { popUpTo(Routes.STANDING) { inclusive = true } } },
            onOpenHelp = { nav.navigate(Routes.S_HELP) },
        )
    }
    composable(Routes.NEXT, listOf(sid)) { entry ->
        val studentId = entry.arguments!!.getLong("sid")
        NextStepScreen(container, meState.value, studentId,
            onHowTo = { rid -> nav.navigate(Routes.howTo(studentId, rid)) },
            onDoneRerun = { nav.navigate(Routes.question(studentId)) { popUpTo(Routes.NEXT) { inclusive = true } } },
            onPath = { nav.navigate(Routes.path(studentId)) },
            onBack = { if (!nav.popBackStack()) nav.navigate(Routes.standing(studentId)) },
        )
    }
    composable(Routes.PATH, listOf(sid)) { entry ->
        val studentId = entry.arguments!!.getLong("sid")
        PathScreen(container, studentId,
            onNextStep = { nav.navigate(Routes.next(studentId)) { launchSingleTop = true } },
            onStanding = { nav.navigate(Routes.standing(studentId)) },
            onStart = { nav.navigate(Routes.S_HOME) },
            onBack = { nav.popBackStack() },
        )
    }
    composable(Routes.HOWTO, listOf(sid, navArgument("rid") { type = NavType.LongType })) { entry ->
        val studentId = entry.arguments!!.getLong("sid")
        val rid = entry.arguments!!.getLong("rid")
        HowToScreen(container, meState.value, studentId, rid,
            onDoneRerun = { nav.navigate(Routes.question(studentId)) { popUpTo(Routes.NEXT) { inclusive = true } } },
            onHelp = { nav.navigate(if (meState.value.role == UserRole.STUDENT) Routes.S_HELP else Routes.cStudent(studentId)) },
            onBack = { nav.popBackStack() },
        )
    }
}

private fun NavGraphBuilder.studentGraph(container: AppContainer, nav: NavHostController, meState: State<UserEntity>) {
    composable(Routes.S_HOME) {
        StudentHomeScreen(container, meState.value,
            onStartIntake = { nav.navigate(Routes.readback(meState.value.id)) },
            onContinueQuestions = { nav.navigate(Routes.question(meState.value.id)) },
            onStanding = { nav.navigate(Routes.standing(meState.value.id)) },
            onNextStep = { nav.navigate(Routes.next(meState.value.id)) },
            onOpening = { nav.navigate(Routes.sOpening(it)) },
        )
    }
    composable(Routes.S_HELP) { StudentHelpScreen(container, meState.value) }
    composable(Routes.S_PROFILE) {
        StudentProfileScreen(container, meState.value, onPath = { nav.navigate(Routes.path(meState.value.id)) })
    }
    composable(Routes.S_OPENING, listOf(navArgument("oid") { type = NavType.LongType })) { entry ->
        OpeningDetailScreen(container, meState.value, entry.arguments!!.getLong("oid"),
            onMadeGoal = { nav.navigate(Routes.question(meState.value.id)) },
            onBack = { nav.popBackStack() },
        )
    }
}

private fun NavGraphBuilder.caseworkerGraph(container: AppContainer, nav: NavHostController, meState: State<UserEntity>) {
    composable(Routes.C_STUDENTS) {
        StudentsScreen(container, meState.value,
            onStudent = { nav.navigate(Routes.cStudent(it)) },
            onAddStudent = { nav.navigate(Routes.C_ADD_STUDENT) },
            onProfile = { nav.navigate(Routes.C_PROFILE) { launchSingleTop = true } },
        )
    }
    composable(Routes.C_REQUESTS) { RequestsScreen(container, meState.value, onStudent = { nav.navigate(Routes.cStudent(it)) }) }
    composable(Routes.C_JOBS) {
        JobsScreen(container, onRole = { nav.navigate(Routes.cRole(it)) }, onOpening = { nav.navigate(Routes.cOpening(it)) })
    }
    composable(Routes.C_PROFILE) { CaseworkerProfileScreen(container, meState.value) }
    composable(Routes.C_STUDENT, listOf(sid)) { entry ->
        val studentId = entry.arguments!!.getLong("sid")
        StudentDetailScreen(container, meState.value, studentId,
            onRunQuestions = { nav.navigate(Routes.question(studentId)) },
            onStartIntake = { nav.navigate(Routes.readback(studentId)) },
            onStanding = { nav.navigate(Routes.standing(studentId)) },
            onBack = { nav.popBackStack() },
        )
    }
    composable(Routes.C_ADD_STUDENT) {
        AddStudentScreen(container, meState.value,
            onCreated = { id, withGoal ->
                nav.navigate(if (withGoal) Routes.readback(id) else Routes.cStudent(id)) { popUpTo(Routes.C_ADD_STUDENT) { inclusive = true } }
            },
            onBack = { nav.popBackStack() },
        )
    }
    composable(Routes.C_ROLE, listOf(navArgument("rid") { type = NavType.LongType })) { entry ->
        RoleEditorScreen(container, meState.value, entry.arguments!!.getLong("rid"), onDone = { nav.popBackStack() })
    }
    composable(Routes.C_OPENING, listOf(navArgument("oid") { type = NavType.LongType })) { entry ->
        OpeningEditorScreen(container, meState.value, entry.arguments!!.getLong("oid"), onDone = { nav.popBackStack() })
    }
}
