package com.skillexchange.ui

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.skillexchange.di.AppContainer
import com.skillexchange.domain.model.NeedPost
import com.skillexchange.domain.model.OfferStatus
import com.skillexchange.domain.model.SwapOffer

private val RuralSkillFilters = listOf("Plumbing", "Electrical", "Carpentry", "Masonry", "Repair", "Farming")

@Composable
fun SkillExchangeRoot(container: AppContainer = remember { AppContainer() }) {
    val factory = remember { SkillExchangeViewModelFactory(container) }
    val authViewModel: AuthViewModel = viewModel(factory = factory)
    val authState by authViewModel.state.collectAsState()

    if (authState.uid == null) {
        LoginScreen(authState, authViewModel)
    } else {
        MainShell(factory)
    }
}

@Composable
private fun LoginScreen(state: AuthUiState, viewModel: AuthViewModel) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("SkillExchange", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
        Text("1 hour = 1 skill point", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = state.phone,
            onValueChange = viewModel::setPhone,
            label = { Text("Phone number with country code") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = { viewModel.sendOtp(context as Activity) },
            enabled = !state.loading && state.phone.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Send OTP")
        }
        if (state.verificationId.isNotBlank()) {
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = state.otp,
                onValueChange = viewModel::setOtp,
                label = { Text("OTP") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            ElevatedButton(
                onClick = viewModel::verifyOtp,
                enabled = !state.loading && state.otp.length >= 6,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Verify and continue")
            }
        }
        state.error?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun MainShell(factory: SkillExchangeViewModelFactory) {
    var tab by remember { mutableStateOf(0) }
    val homeViewModel: HomeViewModel = viewModel(factory = factory)
    val swapViewModel: SwapViewModel = viewModel(factory = factory)
    val chatViewModel: ChatViewModel = viewModel(factory = factory)

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    icon = { Icon(Icons.Default.Search, contentDescription = null) },
                    label = { Text("Needs") }
                )
                NavigationBarItem(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    icon = { Icon(Icons.Default.Handshake, contentDescription = null) },
                    label = { Text("Swaps") }
                )
                NavigationBarItem(
                    selected = tab == 2,
                    onClick = { tab = 2 },
                    icon = { Icon(Icons.Default.Person, contentDescription = null) },
                    label = { Text("Profile") }
                )
            }
        }
    ) { padding ->
        when (tab) {
            0 -> NeedsScreen(Modifier.padding(padding), homeViewModel, swapViewModel)
            1 -> SwapsScreen(Modifier.padding(padding), swapViewModel, chatViewModel)
            else -> ProfileScreen(Modifier.padding(padding), homeViewModel)
        }
    }
}

@Composable
private fun NeedsScreen(modifier: Modifier, home: HomeViewModel, swaps: SwapViewModel) {
    val state by home.state.collectAsState()
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Community needs", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Find nearby service swaps by skill, village, and trust signals.")
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = state.selectedSkill == null,
                    onClick = { home.selectSkill(null) },
                    label = { Text("All") }
                )
                RuralSkillFilters.take(3).forEach { skill ->
                    FilterChip(
                        selected = state.selectedSkill == skill,
                        onClick = { home.selectSkill(skill) },
                        label = { Text(skill) }
                    )
                }
            }
        }
        item { CreateNeedForm(home) }
        if (state.recommendations.isNotEmpty()) {
            item {
                Text("AI recommendations", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            items(state.recommendations) { rec ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.10f))) {
                    Column(Modifier.padding(14.dp)) {
                        Text(rec.title, fontWeight = FontWeight.Bold)
                        Text("${(rec.score * 100).toInt()}% match: ${rec.reason}")
                    }
                }
            }
        }
        item {
            Button(onClick = home::loadRecommendations, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.CheckCircle, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Get AI matches")
            }
        }
        items(state.posts) { post -> NeedPostCard(post, swaps) }
    }
}

@Composable
private fun CreateNeedForm(home: HomeViewModel) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var required by remember { mutableStateOf("Plumbing") }
    var offered by remember { mutableStateOf("Electrical") }
    var village by remember { mutableStateOf("") }
    var hours by remember { mutableFloatStateOf(1f) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Post a need", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        OutlinedTextField(title, { title = it }, label = { Text("Need title") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(description, { description = it }, label = { Text("Details") }, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(required, { required = it }, label = { Text("Need skill") }, modifier = Modifier.weight(1f))
            OutlinedTextField(offered, { offered = it }, label = { Text("Offer skill") }, modifier = Modifier.weight(1f))
        }
        OutlinedTextField(village, { village = it }, label = { Text("Village") }, modifier = Modifier.fillMaxWidth())
        Text("${hours.toInt()} skill point(s)")
        Slider(value = hours, onValueChange = { hours = it }, valueRange = 1f..8f, steps = 6)
        Button(
            onClick = {
                home.createPost(title, description, required, offered, hours.toInt(), village)
                title = ""
                description = ""
            },
            enabled = title.isNotBlank() && description.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Send, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Publish need")
        }
    }
}

@Composable
private fun NeedPostCard(post: NeedPost, swaps: SwapViewModel) {
    var message by remember { mutableStateOf("") }
    Card {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(post.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(post.description)
            Text("${post.requiredSkill} needed | ${post.offeredSkill} offered | ${post.skillPoints} point(s)")
            Text(post.village, color = MaterialTheme.colorScheme.secondary)
            OutlinedTextField(message, { message = it }, label = { Text("Offer message") }, modifier = Modifier.fillMaxWidth())
            Button(onClick = { swaps.offer(post, message) }, enabled = message.isNotBlank()) {
                Text("Send swap offer")
            }
        }
    }
}

@Composable
private fun SwapsScreen(modifier: Modifier, swaps: SwapViewModel, chat: ChatViewModel) {
    val state by swaps.state.collectAsState()
    val chatState by chat.state.collectAsState()
    var draft by remember { mutableStateOf("") }
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Swap offers", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Trust score changes are released only after both people confirm completion.")
        }
        items(state.offers) { offer -> OfferCard(offer, swaps, chat) }
        if (chatState.threadId.isNotBlank()) {
            item {
                Text("Chat", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            items(chatState.messages) { message ->
                Text("${message.senderId.take(6)}: ${message.text}")
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(draft, { draft = it }, label = { Text("Message") }, modifier = Modifier.weight(1f))
                    Button(onClick = {
                        chat.send(draft)
                        draft = ""
                    }) { Text("Send") }
                }
            }
        }
    }
}

@Composable
private fun OfferCard(offer: SwapOffer, swaps: SwapViewModel, chat: ChatViewModel) {
    Card {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("${offer.offeredSkill} for ${offer.requestedSkill}", fontWeight = FontWeight.Bold)
            Text("${offer.estimatedHours} hour(s), ${offer.skillPoints} skill point(s)")
            Text(offer.message)
            Text("Status: ${offer.status}")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (offer.status == OfferStatus.PENDING) {
                    Button(onClick = { swaps.accept(offer.id) }) { Text("Accept") }
                }
                Button(onClick = { swaps.confirm(offer.id) }) { Text("Confirm done") }
                Button(onClick = { chat.open(offer.id) }) { Text("Chat") }
            }
        }
    }
}

@Composable
private fun ProfileScreen(modifier: Modifier, home: HomeViewModel) {
    val state by home.state.collectAsState()
    var name by remember(state.profile?.displayName) { mutableStateOf(state.profile?.displayName.orEmpty()) }
    var village by remember(state.profile?.village) { mutableStateOf(state.profile?.village.orEmpty()) }
    var skills by remember(state.profile?.skills) { mutableStateOf(state.profile?.skills?.joinToString(", ").orEmpty()) }
    var language by remember(state.profile?.languageCode) { mutableStateOf(state.profile?.languageCode ?: "en") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Profile and trust", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Trust score: ${state.profile?.trustScore ?: 50.0}")
        Text("Completed swaps: ${state.profile?.completedSwaps ?: 0}")
        OutlinedTextField(name, { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(village, { village = it }, label = { Text("Village") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(skills, { skills = it }, label = { Text("Skills, comma separated") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(language, { language = it }, label = { Text("Language code") }, modifier = Modifier.fillMaxWidth())
        Button(
            onClick = {
                home.saveProfile(
                    name = name,
                    village = village,
                    skills = skills.split(",").map { it.trim() }.filter { it.isNotBlank() },
                    language = language
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save profile")
        }
    }
}
