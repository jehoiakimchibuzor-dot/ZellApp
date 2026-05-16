package com.example.zell

import androidx.compose.runtime.mutableStateListOf

// SAFETY GUARD: This file must NEVER run in a production (release) build.
// If MockData is ever accidentally used in a release build, this line will
// crash the app immediately during development/testing so you catch it early.
private val debugGuard = check(BuildConfig.DEBUG) {
    "MockData must not be used in production builds! Remove all MockData references before releasing."
}
val mockSpaces = mutableStateListOf(
    SpaceChat("1", "Sarah Okonkwo", "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=200", true),
    SpaceChat("2", "Marcus Obi", "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=200", false),
    SpaceChat("3", "Lena Williams", "https://images.unsplash.com/photo-1438761681033-6461ffad8d80?w=200", true)
)

val mockStories = mutableStateListOf(
    StoryUser(
        id = "1",
        name = "Sarah Okonkwo",
        avatarUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=200",
        stories = listOf(StoryItem(url = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=800"))
    ),
    StoryUser(
        id = "2",
        name = "Marcus Obi",
        avatarUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=200",
        stories = listOf(StoryItem(url = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=800"))
    ),
    StoryUser(
        id = "3",
        name = "Amara Osei",
        avatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=200",
        stories = listOf(StoryItem(url = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=800"))
    ),
    StoryUser(
        id = "4",
        name = "Kofi Mensah",
        avatarUrl = "https://images.unsplash.com/photo-1530268729831-4ca59942d9e2?w=200",
        stories = listOf(StoryItem(url = "https://images.unsplash.com/photo-1530268729831-4ca59942d9e2?w=800"))
    ),
    StoryUser(
        id = "5",
        name = "Zainab Hassan",
        avatarUrl = "https://images.unsplash.com/photo-1438761681033-6461ffad8d80?w=200",
        stories = listOf(StoryItem(url = "https://images.unsplash.com/photo-1438761681033-6461ffad8d80?w=800"))
    ),
    StoryUser(
        id = "6",
        name = "Tunde Adeyemi",
        avatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=200",
        stories = listOf(StoryItem(url = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=800"))
    )
)

val mockFeed = mutableStateListOf(
    FeedPost(
        id = "p1",
        author = "Sarah Okonkwo",
        authorId = "1",
        handle = "@sarah_design",
        avatarUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=200",
        content = "Just finished the new design system for Zell! What do you guys think?",
        imageUrl = "https://images.unsplash.com/photo-1517694712202-14dd9538aa97?w=500",
        likes = 120,
        comments = 15,
        reposts = 5,
        views = "1.2K"
    ),
    FeedPost(
        id = "p2",
        author = "Marcus Obi",
        authorId = "2",
        handle = "@marcus_tech",
        avatarUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=200",
        content = "Zell is going to change how we communicate. Stay tuned! 🚀",
        imageUrl = "https://images.unsplash.com/photo-1552664730-d307ca884978?w=500",
        likes = 85,
        comments = 10,
        reposts = 2,
        views = "850"
    ),
    FeedPost(
        id = "p3",
        author = "Amara Osei",
        authorId = "3",
        handle = "@amara_creative",
        avatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=200",
        content = "Fresh new UI components ready to use! Check them out 💫",
        imageUrl = "https://images.unsplash.com/photo-1561070791-2526d30994b5?w=500",
        likes = 156,
        comments = 22,
        reposts = 8,
        views = "2.3K"
    ),
    FeedPost(
        id = "p4",
        author = "Kofi Mensah",
        authorId = "4",
        handle = "@kofi_creative",
        avatarUrl = "https://images.unsplash.com/photo-1530268729831-4ca59942d9e2?w=200",
        content = "Love working with modern tech stack. Android development has never been better!",
        imageUrl = "https://images.unsplash.com/photo-1517694712202-14dd9538aa97?w=500",
        likes = 234,
        comments = 28,
        reposts = 12,
        views = "3.5K"
    ),
    FeedPost(
        id = "p5",
        author = "Zainab Hassan",
        authorId = "5",
        handle = "@zainab_designs",
        avatarUrl = "https://images.unsplash.com/photo-1438761681033-6461ffad8d80?w=200",
        content = "Excited to announce we're hiring! Check our career page 🎉",
        imageUrl = null,
        likes = 89,
        comments = 34,
        reposts = 15,
        views = "1.8K"
    ),
    FeedPost(
        id = "p6",
        author = "Tunde Adeyemi",
        authorId = "6",
        handle = "@tunde_dev",
        avatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=200",
        content = "Building the future of social media, one pixel at a time ✨",
        imageUrl = "https://images.unsplash.com/photo-1552664730-d307ca884978?w=500",
        likes = 342,
        comments = 45,
        reposts = 20,
        views = "5.2K"
    ),
    FeedPost(
        id = "p7",
        author = "Sarah Okonkwo",
        authorId = "1",
        handle = "@sarah_design",
        avatarUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=200",
        content = "Can't wait for the Zell community to grow! Join us today 🌟",
        imageUrl = "https://images.unsplash.com/photo-1643269865049-fc2b22369578?w=500",
        likes = 567,
        comments = 89,
        reposts = 45,
        views = "8.9K"
    ),
    FeedPost(
        id = "p8",
        author = "Marcus Obi",
        authorId = "2",
        handle = "@marcus_tech",
        avatarUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=200",
        content = "Just published my tutorial on building modern Android apps. Link in bio!",
        imageUrl = null,
        likes = 234,
        comments = 56,
        reposts = 34,
        views = "4.1K"
    ),
    FeedPost(
        id = "p9",
        author = "Amara Osei",
        authorId = "3",
        handle = "@amara_creative",
        avatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=200",
        content = "Design inspiration from nature 🌿 #DesignThinking",
        imageUrl = "https://images.unsplash.com/photo-1586023492125-27b2c045efd7?w=500",
        likes = 678,
        comments = 92,
        reposts = 112,
        views = "12.4K"
    ),
    FeedPost(
        id = "p10",
        author = "Kofi Mensah",
        authorId = "4",
        handle = "@kofi_creative",
        avatarUrl = "https://images.unsplash.com/photo-1530268729831-4ca59942d9e2?w=200",
        content = "New blog post: The future of social media technology",
        imageUrl = "https://images.unsplash.com/photo-1561070791-2526d30994b5?w=500",
        likes = 445,
        comments = 78,
        reposts = 89,
        views = "6.7K"
    ),
    FeedPost(
        id = "p11",
        author = "Zainab Hassan",
        authorId = "5",
        handle = "@zainab_designs",
        avatarUrl = "https://images.unsplash.com/photo-1438761681033-6461ffad8d80?w=200",
        content = "Working on something really cool... stay tuned! 👀",
        imageUrl = "https://images.unsplash.com/photo-1643269865049-fc2b22369578?w=500",
        likes = 912,
        comments = 134,
        reposts = 156,
        views = "15.3K"
    ),
    FeedPost(
        id = "p12",
        author = "Tunde Adeyemi",
        authorId = "6",
        handle = "@tunde_dev",
        avatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=200",
        content = "Grateful for this amazing community! Let's keep building together 💪",
        imageUrl = null,
        likes = 1200,
        comments = 178,
        reposts = 234,
        views = "18.9K"
    )
)

val mockDiscover = listOf(
    DiscoverItem(
        id = "d1",
        title = "Modern Interior Design",
        imageUrl = "https://images.unsplash.com/photo-1586023492125-27b2c045efd7?w=500",
        category = "Interior",
        creatorName = "Design Daily",
        creatorAvatar = "https://i.pravatar.cc/100?u=dd",
        likesCount = 1200
    ),
    DiscoverItem(
        id = "d2",
        title = "Mountain Photography",
        imageUrl = "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?w=500",
        category = "Nature",
        creatorName = "Eco Shots",
        creatorAvatar = "https://i.pravatar.cc/100?u=es",
        likesCount = 850
    ),
    DiscoverItem(
        id = "d3",
        title = "Urban Architecture",
        imageUrl = "https://images.unsplash.com/photo-1486325212027-8081e485255e?w=500",
        category = "Architecture",
        creatorName = "City Explorer",
        creatorAvatar = "https://i.pravatar.cc/100?u=ce",
        likesCount = 2100
    ),
    DiscoverItem(
        id = "d4",
        title = "Food Photography",
        imageUrl = "https://images.unsplash.com/photo-1495521821757-a1efb6729352?w=500",
        category = "Food",
        creatorName = "Taste Lab",
        creatorAvatar = "https://i.pravatar.cc/100?u=tl",
        likesCount = 1650
    ),
    DiscoverItem(
        id = "d5",
        title = "Digital Art Showcase",
        imageUrl = "https://images.unsplash.com/photo-1561070791-2526d30994b5?w=500",
        category = "Art",
        creatorName = "Digital Dreams",
        creatorAvatar = "https://i.pravatar.cc/100?u=dd1",
        likesCount = 3200
    ),
    DiscoverItem(
        id = "d6",
        title = "Fashion Trends 2024",
        imageUrl = "https://images.unsplash.com/photo-1509631179647-0177331693ae?w=500",
        category = "Fashion",
        creatorName = "Style Icon",
        creatorAvatar = "https://i.pravatar.cc/100?u=si",
        likesCount = 2800
    ),
    DiscoverItem(
        id = "d7",
        title = "Travel Adventures",
        imageUrl = "https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=500",
        category = "Travel",
        creatorName = "World Wanderer",
        creatorAvatar = "https://i.pravatar.cc/100?u=ww",
        likesCount = 1950
    ),
    DiscoverItem(
        id = "d8",
        title = "Tech Innovation",
        imageUrl = "https://images.unsplash.com/photo-1677442d019cecf8d90baa4b40c72647e47b1b47f?w=500",
        category = "Technology",
        creatorName = "Tech Hub",
        creatorAvatar = "https://i.pravatar.cc/100?u=th",
        likesCount = 2450
    ),
    DiscoverItem(
        id = "d9",
        title = "Nature's Beauty",
        imageUrl = "https://images.unsplash.com/photo-1441974231531-c6227db76b6e?w=500",
        category = "Nature",
        creatorName = "Green Planet",
        creatorAvatar = "https://i.pravatar.cc/100?u=gp",
        likesCount = 3100
    ),
    DiscoverItem(
        id = "d10",
        title = "Minimalist Design",
        imageUrl = "https://images.unsplash.com/photo-1561070791-2526d30994b5?w=500",
        category = "Design",
        creatorName = "Minimal Studio",
        creatorAvatar = "https://i.pravatar.cc/100?u=ms",
        likesCount = 2650
    ),
    DiscoverItem(
        id = "d11",
        title = "Sport Photography",
        imageUrl = "https://images.unsplash.com/photo-1461896836934-ffe607ba8211?w=500",
        category = "Sports",
        creatorName = "Action Shots",
        creatorAvatar = "https://i.pravatar.cc/100?u=as",
        likesCount = 1780
    ),
    DiscoverItem(
        id = "d12",
        title = "Garden Design",
        imageUrl = "https://images.unsplash.com/photo-1578482326579-147f39cff7ed?w=500",
        category = "Gardening",
        creatorName = "Garden Dreams",
        creatorAvatar = "https://i.pravatar.cc/100?u=gd",
        likesCount = 1420
    )
)
