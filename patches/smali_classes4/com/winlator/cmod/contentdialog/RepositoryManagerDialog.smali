.class public Lcom/winlator/cmod/contentdialog/RepositoryManagerDialog;
.super Ljava/lang/Object;
.source "RepositoryManagerDialog.java"


# annotations
.annotation system Ldalvik/annotation/MemberClasses;
    value = {
        Lcom/winlator/cmod/contentdialog/RepositoryManagerDialog$RepoAdapter;
    }
.end annotation


# instance fields
.field private adapter:Lcom/winlator/cmod/contentdialog/RepositoryManagerDialog$RepoAdapter;

.field private final context:Landroid/content/Context;

.field private dialog:Landroidx/appcompat/app/AlertDialog;

.field private onGlobalDismissCallback:Ljava/lang/Runnable;

.field private recyclerView:Landroidx/recyclerview/widget/RecyclerView;

.field private final repos:Ljava/util/List;
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "Ljava/util/List<",
            "Lcom/winlator/cmod/contentdialog/DriverRepo;",
            ">;"
        }
    .end annotation
.end field


# direct methods
.method public static synthetic $r8$lambda$f6VVjDL57IHK840jLGhuWdUeenk(Lcom/winlator/cmod/contentdialog/RepositoryManagerDialog;Landroid/content/DialogInterface;I)V
    .locals 0

    invoke-direct {p0, p1, p2}, Lcom/winlator/cmod/contentdialog/RepositoryManagerDialog;->lambda$show$0(Landroid/content/DialogInterface;I)V

    return-void
.end method

.method public static synthetic $r8$lambda$wXkUqJuSGHdEt2l6SgKBVp0S2wA(Lcom/winlator/cmod/contentdialog/RepositoryManagerDialog;Landroid/widget/EditText;Landroid/widget/EditText;Lcom/winlator/cmod/contentdialog/DriverRepo;ILandroid/content/DialogInterface;I)V
    .locals 0

    invoke-direct/range {p0 .. p6}, Lcom/winlator/cmod/contentdialog/RepositoryManagerDialog;->lambda$showRepoDialog$1(Landroid/widget/EditText;Landroid/widget/EditText;Lcom/winlator/cmod/contentdialog/DriverRepo;ILandroid/content/DialogInterface;I)V

    return-void
.end method

.method static bridge synthetic -$$Nest$fgetcontext(Lcom/winlator/cmod/contentdialog/RepositoryManagerDialog;)Landroid/content/Context;
    .locals 0

    iget-object p0, p0, Lcom/winlator/cmod/contentdialog/RepositoryManagerDialog;->context:Landroid/content/Context;

    return-object p0
.end method

.method static bridge synthetic -$$Nest$fgetonGlobalDismissCallback(Lcom/winlator/cmod/contentdialog/RepositoryManagerDialog;)Ljava/lang/Runnable;
    .locals 0

    iget-object p0, p0, Lcom/winlator/cmod/contentdialog/RepositoryManagerDialog;->onGlobalDismissCallback:Ljava/lang/Runnable;

    return-object p0
.end method

.method static bridge synthetic -$$Nest$fgetrepos(Lcom/winlator/cmod/contentdialog/RepositoryManagerDialog;)Ljava/util/List;
    .locals 0

    iget-object p0, p0, Lcom/winlator/cmod/contentdialog/RepositoryManagerDialog;->repos:Ljava/util/List;

    return-object p0
.end method

.method static bridge synthetic -$$Nest$msaveRepos(Lcom/winlator/cmod/contentdialog/RepositoryManagerDialog;)V
    .locals 0

    invoke-direct {p0}, Lcom/winlator/cmod/contentdialog/RepositoryManagerDialog;->saveRepos()V

    return-void
.end method

.method static bridge synthetic -$$Nest$mshowRepoDialog(Lcom/winlator/cmod/contentdialog/RepositoryManagerDialog;Lcom/winlator/cmod/contentdialog/DriverRepo;I)V
    .locals 0

    invoke-direct {p0, p1, p2}, Lcom/winlator/cmod/contentdialog/RepositoryManagerDialog;->showRepoDialog(Lcom/winlator/cmod/contentdialog/DriverRepo;I)V

    return-void
.end method

.method public constructor <init>(Landroid/content/Context;)V
    .locals 1
    .param p1, "context"    # Landroid/content/Context;

    .line 36
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    .line 33
    new-instance v0, Ljava/util/ArrayList;

    invoke-direct {v0}, Ljava/util/ArrayList;-><init>()V

    iput-object v0, p0, Lcom/winlator/cmod/contentdialog/RepositoryManagerDialog;->repos:Ljava/util/List;

    .line 37
    iput-object p1, p0, Lcom/winlator/cmod/contentdialog/RepositoryManagerDialog;->context:Landroid/content/Context;

    .line 38
    invoke-direct {p0}, Lcom/winlator/cmod/contentdialog/RepositoryManagerDialog;->loadRepos()V

    .line 39
    return-void
.end method

.method private synthetic lambda$show$0(Landroid/content/DialogInterface;I)V
    .locals 2
    .param p1, "d"    # Landroid/content/DialogInterface;
    .param p2, "w"    # I

    .line 60
    const/4 v0, 0x0

    const/4 v1, -0x1

    invoke-direct {p0, v0, v1}, Lcom/winlator/cmod/contentdialog/RepositoryManagerDialog;->showRepoDialog(Lcom/winlator/cmod/contentdialog/DriverRepo;I)V

    return-void
.end method

.method private synthetic lambda$showRepoDialog$1(Landroid/widget/EditText;Landroid/widget/EditText;Lcom/winlator/cmod/contentdialog/DriverRepo;ILandroid/content/DialogInterface;I)V
    .locals 4
    .param p1, "inputName"    # Landroid/widget/EditText;
    .param p2, "inputUrl"    # Landroid/widget/EditText;
    .param p3, "repoToEdit"    # Lcom/winlator/cmod/contentdialog/DriverRepo;
    .param p4, "position"    # I
    .param p5, "d"    # Landroid/content/DialogInterface;
    .param p6, "w"    # I

    .line 88
    invoke-virtual {p1}, Landroid/widget/EditText;->getText()Landroid/text/Editable;

    move-result-object v0

    invoke-virtual {v0}, Ljava/lang/Object;->toString()Ljava/lang/String;

    move-result-object v0

    invoke-virtual {v0}, Ljava/lang/String;->trim()Ljava/lang/String;

    move-result-object v0

    .line 89
    .local v0, "name":Ljava/lang/String;
    invoke-virtual {p2}, Landroid/widget/EditText;->getText()Landroid/text/Editable;

    move-result-object v1

    invoke-virtual {v1}, Ljava/lang/Object;->toString()Ljava/lang/String;

    move-result-object v1

    invoke-virtual {v1}, Ljava/lang/String;->trim()Ljava/lang/String;

    move-result-object v1

    .line 92
    .local v1, "url":Ljava/lang/String;
    const-string v2, "https://github.com/"

    invoke-virtual {v1, v2}, Ljava/lang/String;->startsWith(Ljava/lang/String;)Z

    move-result v3

    if-eqz v3, :cond_0

    const-string v3, "api.github.com"

    invoke-virtual {v1, v3}, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z

    move-result v3

    if-nez v3, :cond_0

    .line 93
    const-string v3, "https://api.github.com/repos/"

    invoke-virtual {v1, v2, v3}, Ljava/lang/String;->replace(Ljava/lang/CharSequence;Ljava/lang/CharSequence;)Ljava/lang/String;

    move-result-object v1

    .line 94
    const-string v2, "/releases"

    invoke-virtual {v1, v2}, Ljava/lang/String;->endsWith(Ljava/lang/String;)Z

    move-result v3

    if-nez v3, :cond_0

    new-instance v3, Ljava/lang/StringBuilder;

    invoke-direct {v3}, Ljava/lang/StringBuilder;-><init>()V

    invoke-virtual {v3, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v3

    invoke-virtual {v3, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v2

    invoke-virtual {v2}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v1

    .line 97
    :cond_0
    invoke-virtual {v0}, Ljava/lang/String;->isEmpty()Z

    move-result v2

    if-nez v2, :cond_2

    invoke-virtual {v1}, Ljava/lang/String;->isEmpty()Z

    move-result v2

    if-nez v2, :cond_2

    .line 98
    if-nez p3, :cond_1

    .line 99
    iget-object v2, p0, Lcom/winlator/cmod/contentdialog/RepositoryManagerDialog;->repos:Ljava/util/List;

    new-instance v3, Lcom/winlator/cmod/contentdialog/DriverRepo;

    invoke-direct {v3, v0, v1}, Lcom/winlator/cmod/contentdialog/DriverRepo;-><init>(Ljava/lang/String;Ljava/lang/String;)V

    invoke-interface {v2, v3}, Ljava/util/List;->add(Ljava/lang/Object;)Z

    goto :goto_0

    .line 101
    :cond_1
    iput-object v0, p3, Lcom/winlator/cmod/contentdialog/DriverRepo;->name:Ljava/lang/String;

    .line 102
    iput-object v1, p3, Lcom/winlator/cmod/contentdialog/DriverRepo;->apiUrl:Ljava/lang/String;

    .line 103
    iget-object v2, p0, Lcom/winlator/cmod/contentdialog/RepositoryManagerDialog;->repos:Ljava/util/List;

    invoke-interface {v2, p4, p3}, Ljava/util/List;->set(ILjava/lang/Object;)Ljava/lang/Object;

    .line 105
    :goto_0
    invoke-direct {p0}, Lcom/winlator/cmod/contentdialog/RepositoryManagerDialog;->saveRepos()V

    .line 106
    iget-object v2, p0, Lcom/winlator/cmod/contentdialog/RepositoryManagerDialog;->adapter:Lcom/winlator/cmod/contentdialog/RepositoryManagerDialog$RepoAdapter;

    invoke-virtual {v2}, Lcom/winlator/cmod/contentdialog/RepositoryManagerDialog$RepoAdapter;->notifyDataSetChanged()V

    .line 108
    :cond_2
    return-void
.end method

.method private loadRepos()V
    .locals 6

    .line 114
    iget-object v0, p0, Lcom/winlator/cmod/contentdialog/RepositoryManagerDialog;->context:Landroid/content/Context;

    invoke-static {v0}, Landroidx/preference/PreferenceManager;->getDefaultSharedPreferences(Landroid/content/Context;)Landroid/content/SharedPreferences;

    move-result-object v0

    .line 115
    .local v0, "prefs":Landroid/content/SharedPreferences;
    const-string v1, "custom_driver_repos"

    const-string v2, ""

    invoke-interface {v0, v1, v2}, Landroid/content/SharedPreferences;->getString(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    move-result-object v1

    .line 117
    .local v1, "jsonStr":Ljava/lang/String;
    iget-object v2, p0, Lcom/winlator/cmod/contentdialog/RepositoryManagerDialog;->repos:Ljava/util/List;

    invoke-interface {v2}, Ljava/util/List;->clear()V

    .line 118
    invoke-virtual {v1}, Ljava/lang/String;->isEmpty()Z

    move-result v2

    if-eqz v2, :cond_0

    .line 121
    iget-object v2, p0, Lcom/winlator/cmod/contentdialog/RepositoryManagerDialog;->repos:Ljava/util/List;

    new-instance v3, Lcom/winlator/cmod/contentdialog/DriverRepo;

    const-string v4, "Banners Turnip Drivers"

    const-string v5, "https://api.github.com/repos/The412Banner/Banners-Turnip/releases"

    invoke-direct {v3, v4, v5}, Lcom/winlator/cmod/contentdialog/DriverRepo;-><init>(Ljava/lang/String;Ljava/lang/String;)V

    invoke-interface {v2, v3}, Ljava/util/List;->add(Ljava/lang/Object;)Z

    .line 122
    iget-object v2, p0, Lcom/winlator/cmod/contentdialog/RepositoryManagerDialog;->repos:Ljava/util/List;

    new-instance v3, Lcom/winlator/cmod/contentdialog/DriverRepo;

    const-string v4, "K11MCH1 Turnip Drivers"

    const-string v5, "https://api.github.com/repos/K11MCH1/AdrenoToolsDrivers/releases"

    invoke-direct {v3, v4, v5}, Lcom/winlator/cmod/contentdialog/DriverRepo;-><init>(Ljava/lang/String;Ljava/lang/String;)V

    invoke-interface {v2, v3}, Ljava/util/List;->add(Ljava/lang/Object;)Z

    .line 123
    iget-object v2, p0, Lcom/winlator/cmod/contentdialog/RepositoryManagerDialog;->repos:Ljava/util/List;

    new-instance v3, Lcom/winlator/cmod/contentdialog/DriverRepo;

    const-string v4, "StevenMX Turnip Drivers"

    const-string v5, "https://api.github.com/repos/StevenMXZ/freedreno_turnip-CI/releases"

    invoke-direct {v3, v4, v5}, Lcom/winlator/cmod/contentdialog/DriverRepo;-><init>(Ljava/lang/String;Ljava/lang/String;)V

    invoke-interface {v2, v3}, Ljava/util/List;->add(Ljava/lang/Object;)Z

    .line 125
    iget-object v2, p0, Lcom/winlator/cmod/contentdialog/RepositoryManagerDialog;->repos:Ljava/util/List;

    new-instance v3, Lcom/winlator/cmod/contentdialog/DriverRepo;

    const-string v4, "Snapdragon Elite Drivers"

    const-string v5, "https://api.github.com/repos/StevenMXZ/Adrenotools-Drivers/releases"

    invoke-direct {v3, v4, v5}, Lcom/winlator/cmod/contentdialog/DriverRepo;-><init>(Ljava/lang/String;Ljava/lang/String;)V

    invoke-interface {v2, v3}, Ljava/util/List;->add(Ljava/lang/Object;)Z

    .line 127
    iget-object v2, p0, Lcom/winlator/cmod/contentdialog/RepositoryManagerDialog;->repos:Ljava/util/List;

    new-instance v3, Lcom/winlator/cmod/contentdialog/DriverRepo;

    const-string v4, "Weab-Chan Turnip Drivers"

    const-string v5, "https://api.github.com/repos/Weab-chan/freedreno_turnip-CI/releases"

    invoke-direct {v3, v4, v5}, Lcom/winlator/cmod/contentdialog/DriverRepo;-><init>(Ljava/lang/String;Ljava/lang/String;)V

    invoke-interface {v2, v3}, Ljava/util/List;->add(Ljava/lang/Object;)Z

    goto :goto_1

    .line 131
    :cond_0
    :try_start_0
    new-instance v2, Lorg/json/JSONArray;

    invoke-direct {v2, v1}, Lorg/json/JSONArray;-><init>(Ljava/lang/String;)V

    .line 132
    .local v2, "array":Lorg/json/JSONArray;
    const/4 v3, 0x0

    .local v3, "i":I
    :goto_0
    invoke-virtual {v2}, Lorg/json/JSONArray;->length()I

    move-result v4

    if-ge v3, v4, :cond_1

    .line 133
    iget-object v4, p0, Lcom/winlator/cmod/contentdialog/RepositoryManagerDialog;->repos:Ljava/util/List;

    invoke-virtual {v2, v3}, Lorg/json/JSONArray;->getJSONObject(I)Lorg/json/JSONObject;

    move-result-object v5

    invoke-static {v5}, Lcom/winlator/cmod/contentdialog/DriverRepo;->fromJson(Lorg/json/JSONObject;)Lcom/winlator/cmod/contentdialog/DriverRepo;

    move-result-object v5

    invoke-interface {v4, v5}, Ljava/util/List;->add(Ljava/lang/Object;)Z
    :try_end_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_0

    .line 132
    add-int/lit8 v3, v3, 0x1

    goto :goto_0

    .line 137
    .end local v2    # "array":Lorg/json/JSONArray;
    .end local v3    # "i":I
    :cond_1
    goto :goto_1

    .line 135
    :catch_0
    move-exception v2

    .line 139
    :goto_1
    return-void
.end method

.method private saveRepos()V
    .locals 4

    .line 143
    :try_start_0
    new-instance v0, Lorg/json/JSONArray;

    invoke-direct {v0}, Lorg/json/JSONArray;-><init>()V

    .line 144
    .local v0, "array":Lorg/json/JSONArray;
    iget-object v1, p0, Lcom/winlator/cmod/contentdialog/RepositoryManagerDialog;->repos:Ljava/util/List;

    invoke-interface {v1}, Ljava/util/List;->iterator()Ljava/util/Iterator;

    move-result-object v1

    :goto_0
    invoke-interface {v1}, Ljava/util/Iterator;->hasNext()Z

    move-result v2

    if-eqz v2, :cond_0

    invoke-interface {v1}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object v2

    check-cast v2, Lcom/winlator/cmod/contentdialog/DriverRepo;

    .line 145
    .local v2, "repo":Lcom/winlator/cmod/contentdialog/DriverRepo;
    invoke-virtual {v2}, Lcom/winlator/cmod/contentdialog/DriverRepo;->toJson()Lorg/json/JSONObject;

    move-result-object v3

    invoke-virtual {v0, v3}, Lorg/json/JSONArray;->put(Ljava/lang/Object;)Lorg/json/JSONArray;

    .line 146
    nop

    .end local v2    # "repo":Lcom/winlator/cmod/contentdialog/DriverRepo;
    goto :goto_0

    .line 147
    :cond_0
    iget-object v1, p0, Lcom/winlator/cmod/contentdialog/RepositoryManagerDialog;->context:Landroid/content/Context;

    invoke-static {v1}, Landroidx/preference/PreferenceManager;->getDefaultSharedPreferences(Landroid/content/Context;)Landroid/content/SharedPreferences;

    move-result-object v1

    .line 148
    invoke-interface {v1}, Landroid/content/SharedPreferences;->edit()Landroid/content/SharedPreferences$Editor;

    move-result-object v1

    const-string v2, "custom_driver_repos"

    .line 149
    invoke-virtual {v0}, Lorg/json/JSONArray;->toString()Ljava/lang/String;

    move-result-object v3

    invoke-interface {v1, v2, v3}, Landroid/content/SharedPreferences$Editor;->putString(Ljava/lang/String;Ljava/lang/String;)Landroid/content/SharedPreferences$Editor;

    move-result-object v1

    .line 150
    invoke-interface {v1}, Landroid/content/SharedPreferences$Editor;->apply()V
    :try_end_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_0

    .line 153
    .end local v0    # "array":Lorg/json/JSONArray;
    goto :goto_1

    .line 151
    :catch_0
    move-exception v0

    .line 152
    .local v0, "e":Ljava/lang/Exception;
    invoke-virtual {v0}, Ljava/lang/Exception;->printStackTrace()V

    .line 154
    .end local v0    # "e":Ljava/lang/Exception;
    :goto_1
    return-void
.end method

.method private showRepoDialog(Lcom/winlator/cmod/contentdialog/DriverRepo;I)V
    .locals 11
    .param p1, "repoToEdit"    # Lcom/winlator/cmod/contentdialog/DriverRepo;
    .param p2, "position"    # I

    .line 69
    new-instance v0, Landroidx/appcompat/app/AlertDialog$Builder;

    iget-object v1, p0, Lcom/winlator/cmod/contentdialog/RepositoryManagerDialog;->context:Landroid/content/Context;

    invoke-direct {v0, v1}, Landroidx/appcompat/app/AlertDialog$Builder;-><init>(Landroid/content/Context;)V

    .line 70
    .local v0, "builder":Landroidx/appcompat/app/AlertDialog$Builder;
    if-nez p1, :cond_0

    const-string v1, "Add Repository"

    goto :goto_0

    :cond_0
    const-string v1, "Edit Repository"

    :goto_0
    invoke-virtual {v0, v1}, Landroidx/appcompat/app/AlertDialog$Builder;->setTitle(Ljava/lang/CharSequence;)Landroidx/appcompat/app/AlertDialog$Builder;

    .line 72
    new-instance v1, Landroid/widget/EditText;

    iget-object v2, p0, Lcom/winlator/cmod/contentdialog/RepositoryManagerDialog;->context:Landroid/content/Context;

    invoke-direct {v1, v2}, Landroid/widget/EditText;-><init>(Landroid/content/Context;)V

    .line 73
    .local v1, "inputName":Landroid/widget/EditText;
    const-string v2, "Name (e.g. Kimchi Turnip)"

    invoke-virtual {v1, v2}, Landroid/widget/EditText;->setHint(Ljava/lang/CharSequence;)V

    .line 74
    if-eqz p1, :cond_1

    iget-object v2, p1, Lcom/winlator/cmod/contentdialog/DriverRepo;->name:Ljava/lang/String;

    invoke-virtual {v1, v2}, Landroid/widget/EditText;->setText(Ljava/lang/CharSequence;)V

    .line 76
    :cond_1
    new-instance v2, Landroid/widget/EditText;

    iget-object v3, p0, Lcom/winlator/cmod/contentdialog/RepositoryManagerDialog;->context:Landroid/content/Context;

    invoke-direct {v2, v3}, Landroid/widget/EditText;-><init>(Landroid/content/Context;)V

    .line 77
    .local v2, "inputUrl":Landroid/widget/EditText;
    const-string v3, "GitHub API URL"

    invoke-virtual {v2, v3}, Landroid/widget/EditText;->setHint(Ljava/lang/CharSequence;)V

    .line 78
    if-eqz p1, :cond_2

    iget-object v3, p1, Lcom/winlator/cmod/contentdialog/DriverRepo;->apiUrl:Ljava/lang/String;

    invoke-virtual {v2, v3}, Landroid/widget/EditText;->setText(Ljava/lang/CharSequence;)V

    .line 80
    :cond_2
    new-instance v3, Landroid/widget/LinearLayout;

    iget-object v4, p0, Lcom/winlator/cmod/contentdialog/RepositoryManagerDialog;->context:Landroid/content/Context;

    invoke-direct {v3, v4}, Landroid/widget/LinearLayout;-><init>(Landroid/content/Context;)V

    move-object v9, v3

    .line 81
    .local v9, "layout":Landroid/widget/LinearLayout;
    const/4 v3, 0x1

    invoke-virtual {v9, v3}, Landroid/widget/LinearLayout;->setOrientation(I)V

    .line 82
    const/16 v3, 0x32

    const/16 v4, 0x1e

    invoke-virtual {v9, v3, v4, v3, v4}, Landroid/widget/LinearLayout;->setPadding(IIII)V

    .line 83
    invoke-virtual {v9, v1}, Landroid/widget/LinearLayout;->addView(Landroid/view/View;)V

    .line 84
    invoke-virtual {v9, v2}, Landroid/widget/LinearLayout;->addView(Landroid/view/View;)V

    .line 85
    invoke-virtual {v0, v9}, Landroidx/appcompat/app/AlertDialog$Builder;->setView(Landroid/view/View;)Landroidx/appcompat/app/AlertDialog$Builder;

    .line 87
    new-instance v10, Lcom/winlator/cmod/contentdialog/RepositoryManagerDialog$$ExternalSyntheticLambda1;

    move-object v3, v10

    move-object v4, p0

    move-object v5, v1

    move-object v6, v2

    move-object v7, p1

    move v8, p2

    invoke-direct/range {v3 .. v8}, Lcom/winlator/cmod/contentdialog/RepositoryManagerDialog$$ExternalSyntheticLambda1;-><init>(Lcom/winlator/cmod/contentdialog/RepositoryManagerDialog;Landroid/widget/EditText;Landroid/widget/EditText;Lcom/winlator/cmod/contentdialog/DriverRepo;I)V

    const-string v3, "Save"

    invoke-virtual {v0, v3, v10}, Landroidx/appcompat/app/AlertDialog$Builder;->setPositiveButton(Ljava/lang/CharSequence;Landroid/content/DialogInterface$OnClickListener;)Landroidx/appcompat/app/AlertDialog$Builder;

    .line 109
    const-string v3, "Cancel"

    const/4 v4, 0x0

    invoke-virtual {v0, v3, v4}, Landroidx/appcompat/app/AlertDialog$Builder;->setNegativeButton(Ljava/lang/CharSequence;Landroid/content/DialogInterface$OnClickListener;)Landroidx/appcompat/app/AlertDialog$Builder;

    .line 110
    invoke-virtual {v0}, Landroidx/appcompat/app/AlertDialog$Builder;->show()Landroidx/appcompat/app/AlertDialog;

    .line 111
    return-void
.end method


# virtual methods
.method public setOnDismissCallback(Ljava/lang/Runnable;)V
    .locals 0
    .param p1, "callback"    # Ljava/lang/Runnable;

    .line 42
    iput-object p1, p0, Lcom/winlator/cmod/contentdialog/RepositoryManagerDialog;->onGlobalDismissCallback:Ljava/lang/Runnable;

    .line 43
    return-void
.end method

.method public show()V
    .locals 5

    .line 46
    new-instance v0, Landroidx/appcompat/app/AlertDialog$Builder;

    iget-object v1, p0, Lcom/winlator/cmod/contentdialog/RepositoryManagerDialog;->context:Landroid/content/Context;

    invoke-direct {v0, v1}, Landroidx/appcompat/app/AlertDialog$Builder;-><init>(Landroid/content/Context;)V

    .line 47
    .local v0, "builder":Landroidx/appcompat/app/AlertDialog$Builder;
    const-string v1, "Driver Sources"

    invoke-virtual {v0, v1}, Landroidx/appcompat/app/AlertDialog$Builder;->setTitle(Ljava/lang/CharSequence;)Landroidx/appcompat/app/AlertDialog$Builder;

    .line 49
    new-instance v1, Landroidx/recyclerview/widget/RecyclerView;

    iget-object v2, p0, Lcom/winlator/cmod/contentdialog/RepositoryManagerDialog;->context:Landroid/content/Context;

    invoke-direct {v1, v2}, Landroidx/recyclerview/widget/RecyclerView;-><init>(Landroid/content/Context;)V

    iput-object v1, p0, Lcom/winlator/cmod/contentdialog/RepositoryManagerDialog;->recyclerView:Landroidx/recyclerview/widget/RecyclerView;

    .line 50
    iget-object v1, p0, Lcom/winlator/cmod/contentdialog/RepositoryManagerDialog;->recyclerView:Landroidx/recyclerview/widget/RecyclerView;

    new-instance v2, Landroidx/recyclerview/widget/LinearLayoutManager;

    iget-object v3, p0, Lcom/winlator/cmod/contentdialog/RepositoryManagerDialog;->context:Landroid/content/Context;

    invoke-direct {v2, v3}, Landroidx/recyclerview/widget/LinearLayoutManager;-><init>(Landroid/content/Context;)V

    invoke-virtual {v1, v2}, Landroidx/recyclerview/widget/RecyclerView;->setLayoutManager(Landroidx/recyclerview/widget/RecyclerView$LayoutManager;)V

    .line 51
    iget-object v1, p0, Lcom/winlator/cmod/contentdialog/RepositoryManagerDialog;->recyclerView:Landroidx/recyclerview/widget/RecyclerView;

    new-instance v2, Landroidx/recyclerview/widget/DividerItemDecoration;

    iget-object v3, p0, Lcom/winlator/cmod/contentdialog/RepositoryManagerDialog;->context:Landroid/content/Context;

    const/4 v4, 0x1

    invoke-direct {v2, v3, v4}, Landroidx/recyclerview/widget/DividerItemDecoration;-><init>(Landroid/content/Context;I)V

    invoke-virtual {v1, v2}, Landroidx/recyclerview/widget/RecyclerView;->addItemDecoration(Landroidx/recyclerview/widget/RecyclerView$ItemDecoration;)V

    .line 53
    iget-object v1, p0, Lcom/winlator/cmod/contentdialog/RepositoryManagerDialog;->recyclerView:Landroidx/recyclerview/widget/RecyclerView;

    const/4 v2, 0x0

    const/16 v3, 0xa

    invoke-virtual {v1, v2, v3, v2, v3}, Landroidx/recyclerview/widget/RecyclerView;->setPadding(IIII)V

    .line 55
    new-instance v1, Lcom/winlator/cmod/contentdialog/RepositoryManagerDialog$RepoAdapter;

    const/4 v2, 0x0

    invoke-direct {v1, p0, v2}, Lcom/winlator/cmod/contentdialog/RepositoryManagerDialog$RepoAdapter;-><init>(Lcom/winlator/cmod/contentdialog/RepositoryManagerDialog;Lcom/winlator/cmod/contentdialog/RepositoryManagerDialog-IA;)V

    iput-object v1, p0, Lcom/winlator/cmod/contentdialog/RepositoryManagerDialog;->adapter:Lcom/winlator/cmod/contentdialog/RepositoryManagerDialog$RepoAdapter;

    .line 56
    iget-object v1, p0, Lcom/winlator/cmod/contentdialog/RepositoryManagerDialog;->recyclerView:Landroidx/recyclerview/widget/RecyclerView;

    iget-object v3, p0, Lcom/winlator/cmod/contentdialog/RepositoryManagerDialog;->adapter:Lcom/winlator/cmod/contentdialog/RepositoryManagerDialog$RepoAdapter;

    invoke-virtual {v1, v3}, Landroidx/recyclerview/widget/RecyclerView;->setAdapter(Landroidx/recyclerview/widget/RecyclerView$Adapter;)V

    .line 58
    iget-object v1, p0, Lcom/winlator/cmod/contentdialog/RepositoryManagerDialog;->recyclerView:Landroidx/recyclerview/widget/RecyclerView;

    invoke-virtual {v0, v1}, Landroidx/appcompat/app/AlertDialog$Builder;->setView(Landroid/view/View;)Landroidx/appcompat/app/AlertDialog$Builder;

    .line 60
    new-instance v1, Lcom/winlator/cmod/contentdialog/RepositoryManagerDialog$$ExternalSyntheticLambda0;

    invoke-direct {v1, p0}, Lcom/winlator/cmod/contentdialog/RepositoryManagerDialog$$ExternalSyntheticLambda0;-><init>(Lcom/winlator/cmod/contentdialog/RepositoryManagerDialog;)V

    const-string v3, "Add Source"

    invoke-virtual {v0, v3, v1}, Landroidx/appcompat/app/AlertDialog$Builder;->setPositiveButton(Ljava/lang/CharSequence;Landroid/content/DialogInterface$OnClickListener;)Landroidx/appcompat/app/AlertDialog$Builder;

    .line 61
    const-string v1, "Close"

    invoke-virtual {v0, v1, v2}, Landroidx/appcompat/app/AlertDialog$Builder;->setNegativeButton(Ljava/lang/CharSequence;Landroid/content/DialogInterface$OnClickListener;)Landroidx/appcompat/app/AlertDialog$Builder;

    .line 63
    invoke-virtual {v0}, Landroidx/appcompat/app/AlertDialog$Builder;->create()Landroidx/appcompat/app/AlertDialog;

    move-result-object v1

    iput-object v1, p0, Lcom/winlator/cmod/contentdialog/RepositoryManagerDialog;->dialog:Landroidx/appcompat/app/AlertDialog;

    .line 64
    iget-object v1, p0, Lcom/winlator/cmod/contentdialog/RepositoryManagerDialog;->dialog:Landroidx/appcompat/app/AlertDialog;

    invoke-virtual {v1}, Landroidx/appcompat/app/AlertDialog;->show()V

    .line 65
    return-void
.end method
