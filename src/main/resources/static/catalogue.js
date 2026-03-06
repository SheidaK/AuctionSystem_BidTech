// API Base URL
const API_BASE = 'http://localhost:5000/api/catalogue';

// Load products on page load
document.addEventListener('DOMContentLoaded', () => {
    loadProducts();
    
    // Add event listeners
    document.getElementById('searchInput').addEventListener('input', filterProducts);
    document.getElementById('categoryFilter').addEventListener('change', filterProducts);
    document.getElementById('statusFilter').addEventListener('change', filterProducts);
});

// Global products array
let allProducts = [];

// Load all products from API
async function loadProducts() {
    const container = document.getElementById('productsContainer');
    container.innerHTML = '<div class="loading">Loading products...</div>';
    
    try {
        const response = await fetch(`${API_BASE}/products`);
        
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        
        allProducts = await response.json();
        displayProducts(allProducts);
    } catch (error) {
        console.error('Error loading products:', error);
        container.innerHTML = `
            <div class="error">
                <strong>Error loading products:</strong> ${error.message}
                <br><br>
                Make sure the backend server is running on port 5000.
            </div>
        `;
    }
}

// Display products in grid
function displayProducts(products) {
    const container = document.getElementById('productsContainer');
    
    if (products.length === 0) {
        container.innerHTML = `
            <div class="empty-state">
                <div class="empty-state-icon">📦</div>
                <h2>No products found</h2>
                <p>Try adjusting your filters or add a new product</p>
            </div>
        `;
        return;
    }
    
    container.innerHTML = products.map(product => `
        <div class="product-card" onclick="showProductDetails(${product.id})">
            <div class="product-image">
                ${getProductIcon(product.category)}
            </div>
            <div class="product-info">
                <span class="category-tag">${product.category}</span>
                <div class="product-name">${escapeHtml(product.name)}</div>
                <div class="product-description">${escapeHtml(product.description || 'No description')}</div>
                <div class="product-details">
                    <div class="price">$${product.startingPrice.toFixed(2)}</div>
                    <span class="badge badge-${product.status.toLowerCase()}">${product.status}</span>
                </div>
            </div>
        </div>
    `).join('');
}

// Filter products based on search and filters
function filterProducts() {
    const searchTerm = document.getElementById('searchInput').value.toLowerCase();
    const categoryFilter = document.getElementById('categoryFilter').value;
    const statusFilter = document.getElementById('statusFilter').value;
    
    const filtered = allProducts.filter(product => {
        const matchesSearch = product.name.toLowerCase().includes(searchTerm) ||
                            (product.description && product.description.toLowerCase().includes(searchTerm)) ||
                            (product.keywords && product.keywords.toLowerCase().includes(searchTerm));
        
        const matchesCategory = !categoryFilter || product.category === categoryFilter;
        const matchesStatus = !statusFilter || product.status === statusFilter;
        
        return matchesSearch && matchesCategory && matchesStatus;
    });
    
    displayProducts(filtered);
}

// Show product details in modal
async function showProductDetails(productId) {
    try {
        const response = await fetch(`${API_BASE}/products/${productId}`);
        const product = await response.json();
        
        const modalBody = document.getElementById('modalBody');
        modalBody.innerHTML = `
            <div class="detail-row">
                <div class="detail-label">Product Name</div>
                <div class="detail-value">${escapeHtml(product.name)}</div>
            </div>
            <div class="detail-row">
                <div class="detail-label">Description</div>
                <div class="detail-value">${escapeHtml(product.description || 'No description')}</div>
            </div>
            <div class="detail-row">
                <div class="detail-label">Category</div>
                <div class="detail-value">${product.category}</div>
            </div>
            <div class="detail-row">
                <div class="detail-label">Starting Price</div>
                <div class="detail-value">$${product.startingPrice.toFixed(2)}</div>
            </div>
            <div class="detail-row">
                <div class="detail-label">Reserve Price</div>
                <div class="detail-value">$${product.reservePrice.toFixed(2)}</div>
            </div>
            <div class="detail-row">
                <div class="detail-label">Shipping Cost</div>
                <div class="detail-value">$${product.shippingCost ? product.shippingCost.toFixed(2) : '0.00'}</div>
            </div>
            <div class="detail-row">
                <div class="detail-label">End Date</div>
                <div class="detail-value">${product.endDate ? new Date(product.endDate).toLocaleString() : 'Not set'}</div>
            </div>
            <div class="detail-row">
                <div class="detail-label">Status</div>
                <div class="detail-value">
                    <span class="badge badge-${product.status.toLowerCase()}">${product.status}</span>
                </div>
            </div>
            <div class="detail-row">
                <div class="detail-label">Condition</div>
                <div class="detail-value">${product.condition || 'Not specified'}</div>
            </div>
            <div class="detail-row">
                <div class="detail-label">Keywords</div>
                <div class="detail-value">${product.keywords || 'None'}</div>
            </div>
            <div class="detail-row">
                <div class="detail-label">Created</div>
                <div class="detail-value">${new Date(product.createdAt).toLocaleString()}</div>
            </div>
            ${product.status === 'DRAFT' || product.status === 'INACTIVE' ? `
                <div class="form-actions">
                    <button class="btn btn-primary" onclick="activateProduct(${product.id})">Activate Product</button>
                    <button class="btn btn-secondary" onclick="deleteProduct(${product.id})">Delete</button>
                </div>
            ` : ''}
        `;
        
        document.getElementById('productModal').classList.add('active');
    } catch (error) {
        console.error('Error loading product details:', error);
        alert('Error loading product details');
    }
}

// Show add product modal
function showAddProductModal() {
    document.getElementById('addProductForm').reset();
    document.getElementById('addProductModal').classList.add('active');
}

// Add new product
async function addProduct(event) {
    event.preventDefault();
    
    const form = event.target;
    const formData = new FormData(form);
    
    const productData = {
        name: formData.get('name'),
        description: formData.get('description'),
        category: formData.get('category'),
        startingPrice: parseFloat(formData.get('startingPrice')),
        reservePrice: parseFloat(formData.get('reservePrice')),
        shippingCost: parseFloat(formData.get('shippingCost')),
        endDate: new Date(formData.get('endDate')).toISOString(),
        keywords: formData.get('keywords'),
        condition: formData.get('condition'),
        sellerId: 1, // Default seller ID
        quantity: 1,
        auctionType: 'English Auction'
    };
    
    try {
        const response = await fetch(`${API_BASE}/products`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(productData)
        });
        
        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message || 'Failed to add product');
        }
        
        alert('Product added successfully!');
        closeAddModal();
        loadProducts();
    } catch (error) {
        console.error('Error adding product:', error);
        alert('Error adding product: ' + error.message);
    }
}

// Activate product
async function activateProduct(productId) {
    try {
        const response = await fetch(`${API_BASE}/products/${productId}/activate`, {
            method: 'POST'
        });
        
        if (!response.ok) {
            throw new Error('Failed to activate product');
        }
        
        alert('Product activated successfully!');
        closeModal();
        loadProducts();
    } catch (error) {
        console.error('Error activating product:', error);
        alert('Error activating product: ' + error.message);
    }
}

// Delete product
async function deleteProduct(productId) {
    if (!confirm('Are you sure you want to delete this product?')) {
        return;
    }
    
    try {
        const response = await fetch(`${API_BASE}/products/${productId}`, {
            method: 'DELETE'
        });
        
        if (!response.ok) {
            throw new Error('Failed to delete product');
        }
        
        alert('Product deleted successfully!');
        closeModal();
        loadProducts();
    } catch (error) {
        console.error('Error deleting product:', error);
        alert('Error deleting product: ' + error.message);
    }
}

// Close modals
function closeModal() {
    document.getElementById('productModal').classList.remove('active');
}

function closeAddModal() {
    document.getElementById('addProductModal').classList.remove('active');
}

// Get icon for product category
function getProductIcon(category) {
    const icons = {
        'Electronics': '💻',
        'Jewelry': '💎',
        'Art': '🎨',
        'Books': '📚',
        'Other': '📦'
    };
    return icons[category] || '📦';
}

// Escape HTML to prevent XSS
function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

// Close modal when clicking outside
window.onclick = function(event) {
    const productModal = document.getElementById('productModal');
    const addModal = document.getElementById('addProductModal');
    
    if (event.target === productModal) {
        closeModal();
    }
    if (event.target === addModal) {
        closeAddModal();
    }
}
