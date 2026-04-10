package com.mienaiknife.narra.data.models

object SampleArticles {
    val sampleArticle1 = Article(
        id = "1",
        title = "Why training AI can't be IP theft",
        source = "blog.giovanh.com",
        publishedAt = "Apr 3, 2025",
        content = """
            AI is a huge subject, so it’s hard to boil my thoughts down into any single digestible take. That’s probably a good thing. As a rule, if you can fit your understanding of something complex into a tweet, you’re usually wrong. So I’m continuing to divide and conquer here, eat the elephant one bite at a time, etc.

            Right now I want to address one specific question: whether people have the right to train AI in the first place. The argument that they do not¹ goes like this:

            > When a corporation trains generative AI they have unfairly used other people’s work without consent or compensation to create a new product they own. Worse, the new product directly competes with the original workers. Since the corporations didn’t own the original material and weren’t granted any specific rights to use it for training, they do not have the right to train with it. When the
        """.trimIndent(),
        progress = 0.8f,
        isFavorite = false,
        isFromFeed = false,
        isInQueue = true
    )

    val sampleArticle2 = Article(
        id = "2",
        title = "Modern Android Development",
        source = "Android Developers",
        publishedAt = "Mar 20, 2025",
        content = "Jetpack Compose is the modern toolkit for building native UI on Android...",
        progress = 0.5f,
        isFavorite = true,
        isFromFeed = true,
        isInQueue = true
    )

    val sampleArticle3 = Article(
        id = "3",
        title = "The Future of RSS",
        source = "The Verge",
        publishedAt = "Feb 15, 2025",
        content = "Is the open web making a comeback? Some say RSS is the key to escaping algorithmic feeds...",
        isFavorite = false,
        isFromFeed = true,
        isInQueue = false
    )

    val finishedArticle = Article(
        id = "4",
        title = "Sustainable Web Design",
        source = "A List Apart",
        publishedAt = "Jan 10, 2025",
        content = "",
        progress = 1.0f,
        isFavorite = true,
        isFromFeed = false,
        isInQueue = false
    )

    val all = listOf(sampleArticle1, sampleArticle2, sampleArticle3, finishedArticle)
}
