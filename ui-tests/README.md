# 2Auth UI Testing with Playwright

Simple, powerful automation testing for the 2Auth UI using Playwright - the most popular and easy-to-learn testing framework.

## 🎭 Why Playwright?

- **Easy to Learn** - Simple, intuitive API
- **Most Popular** - Used by Microsoft, Google, and thousands of companies
- **Cross-Browser** - Chrome, Firefox, Safari support
- **Mobile Testing** - Built-in device emulation
- **Fast & Reliable** - Auto-waiting and smart assertions
- **Great Debugging** - Visual traces and time-travel debugging

## 🚀 Quick Start

1. **Install dependencies:**
   ```bash
   npm install
   ```

2. **Install browsers:**
   ```bash
   npm run setup
   ```

3. **Start the server (in one terminal):**
   ```bash
   npm run serve
   ```

4. **Run tests (in another terminal):**
   ```bash
   npm test
   ```

## 📋 What's Tested

### 🔐 Authentication Flow
- Login with username/password
- Two-factor authentication (TOTP)
- Form validation and error handling
- Session management

### 📊 Dashboard Features
- User management (create, edit, delete)
- Statistics display
- Modal interactions
- Tab navigation

### 📱 Responsive Design
- Mobile viewport (375px)
- Tablet viewport (768px) 
- Desktop viewport (1920px)
- Bootstrap grid behavior

### ♿ Accessibility
- Keyboard navigation
- ARIA attributes
- Color contrast
- Focus management

## 🎯 Running Tests

```bash
# Basic test run
npm test

# With browser UI (see what's happening)
npm run test:headed

# Debug mode (step-by-step)
npm run test:debug

# Interactive UI mode
npm run test:ui

# View test report
npm run test:report
```

## 📊 Test Reports

After running tests, view the interactive HTML report:

```bash
npm run test:report
```

Reports include:
- ✅ Test results and screenshots
- 🎬 Video recordings of failures
- 🔍 Step-by-step traces for debugging
- 📈 Performance metrics

## 🔧 Configuration

The `playwright.config.js` file controls:

- **Multi-browser testing** - Chrome, Firefox, Safari
- **Device simulation** - Mobile, tablet, desktop
- **Network conditions** - Slow 3G, WiFi, etc.
- **Screenshot/video capture** - On failures
- **Parallel execution** - Faster test runs

## 🐛 Easy Debugging

Playwright makes debugging simple:

```bash
# Run in debug mode
npm run test:debug

# View trace files
npx playwright show-trace test-results/trace.zip
```

## ✅ What This Tests

Your UI is validated for:

- 🔐 **Security** - Login, TOTP, form validation
- 📱 **Responsiveness** - Works on all device sizes  
- ♿ **Accessibility** - Keyboard navigation, ARIA
- 🎨 **UI/UX** - Modals, tabs, error handling
- ⚡ **Performance** - Page load times
- 🌐 **Cross-browser** - Chrome, Firefox, Safari

## 🎉 Success!

Your static UI now has professional-grade automated testing with **Playwright** - the easiest and most popular testing framework!

**Key Benefits:**
- ✅ Catches bugs before users do
- ✅ Ensures consistent behavior across browsers
- ✅ Validates accessibility compliance
- ✅ Confirms responsive design works
- ✅ Provides confidence when making changes

---

*Happy testing! 🚀*
